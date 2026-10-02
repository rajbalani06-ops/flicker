import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, Subject, Subscription, switchMap, timer } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { Conversation, DirectMessage, MessageService } from '../../core/services/message.service';
import { UserSearchResult, UserService } from '../../core/services/user.service';

@Component({ selector: 'app-messages', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './messages.html', styleUrl: './messages.css' })
export class MessagesComponent implements OnInit, OnDestroy {
  @ViewChild('history') private historyElement?: ElementRef<HTMLElement>;
  conversations = signal<Conversation[]>([]);
  messages = signal<DirectMessage[]>([]);
  selected = signal<Conversation | null>(null);
  searchResults = signal<UserSearchResult[]>([]);
  searchText = '';
  draft = '';
  loading = signal(false);
  error = signal('');
  searchOpen = signal(false);
  menuOpen = signal(false);
  messageToDelete = signal<DirectMessage | null>(null);
  clearConfirmation = signal(false);
  mobileChatOpen = signal(false);
  private query$ = new Subject<string>();
  private stop$ = new Subject<void>();
  private refreshSub?: Subscription;
  private listSub?: Subscription;
  private searchSub?: Subscription;
  private nextTempId = -1;

  constructor(private service: MessageService, private users: UserService, public auth: AuthService) {}

  ngOnInit(): void {
    this.loadConversations();
    this.listSub = timer(10000, 10000).pipe(switchMap(() => this.service.conversations().pipe(catchError(() => of([]))))).subscribe(rows => this.mergeConversations(rows));
    this.searchSub = this.query$.pipe(debounceTime(300), distinctUntilChanged(), switchMap(query => query.length < 2 ? of([]) : this.users.searchUsers(query).pipe(catchError(() => of([]))))).subscribe(rows => this.searchResults.set(rows));
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe(); this.listSub?.unsubscribe(); this.searchSub?.unsubscribe();
    this.stop$.next(); this.stop$.complete(); this.query$.complete();
  }

  loadConversations(): void {
    this.service.conversations().subscribe({ next: rows => this.mergeConversations(rows || []), error: () => this.error.set('Could not load conversations.') });
  }

  private mergeConversations(rows: Conversation[]): void {
    const active = this.selected();
    this.conversations.set(rows);
    if (active) {
      const updated = rows.find(row => row.id === active.id);
      if (updated) this.selected.set(updated);
    }
  }

  searchChanged(value: string): void { this.searchText = value; this.searchOpen.set(true); this.query$.next(value.trim()); }

  chooseUser(user: UserSearchResult): void {
    this.searchOpen.set(false); this.searchText = ''; this.searchResults.set([]);
    const existing = this.conversations().find(row => row.username.toLowerCase() === user.username.toLowerCase());
    if (existing) { this.open(existing); return; }
    this.error.set('');
    this.service.startConversation(user.username).subscribe({ next: conversation => { this.mergeConversations([conversation, ...this.conversations().filter(row => row.id !== conversation.id)]); this.open(conversation); }, error: () => this.error.set('Could not start a conversation. Please try again.') });
  }

  open(conversation: Conversation): void {
    const pendingUnread = conversation.unreadCount || 0;
    if (pendingUnread > 0) {
      this.conversations.update(rows => rows.map(row => row.id === conversation.id ? { ...row, unreadCount: 0 } : row));
      conversation = { ...conversation, unreadCount: 0 };
      this.service.decrementUnread(pendingUnread);
    }
    this.service.markRead(conversation.id).subscribe({ error: () => undefined });
    this.refreshSub?.unsubscribe();
    this.selected.set(conversation); this.mobileChatOpen.set(true); this.loading.set(true); this.messages.set([]); this.error.set('');
    this.refreshSub = timer(0, 3000).pipe(switchMap(() => this.service.history(conversation.id).pipe(catchError(() => { this.error.set('Could not load messages.'); return of(null); })))).subscribe(rows => {
      if (rows === null) { this.loading.set(false); return; }
      const pending = this.messages().filter(message => message.id < 0);
      const nextRows = [...rows, ...pending];
      const changed = this.messages().length !== nextRows.length || this.messages().at(-1)?.id !== nextRows.at(-1)?.id;
      this.messages.set(nextRows); this.loading.set(false);
      if (changed) this.scrollToBottom();
      this.loadConversations();
    });
  }

  send(): void {
    const conversation = this.selected(), content = this.draft.trim();
    if (!conversation || !content) return;
    const temporary: DirectMessage = { id: this.nextTempId--, senderUsername: this.auth.currentUser() || '', content, createdAt: new Date().toISOString() };
    this.draft = ''; this.messages.update(rows => [...rows, temporary]); this.scrollToBottom();
    this.service.send(conversation.id, content).subscribe({ next: message => { this.messages.update(rows => rows.some(row => row.id === temporary.id) ? rows.map(row => row.id === temporary.id ? message : row) : [...rows, message]); this.loadConversations(); this.scrollToBottom(); }, error: () => { this.messages.update(rows => rows.filter(row => row.id !== temporary.id)); this.draft = content; this.error.set('Message could not be sent. Your text is back in the composer.'); } });
  }

  onComposerKeydown(event: KeyboardEvent): void { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); this.send(); } }
  mine(message: DirectMessage): boolean { return message.senderUsername.toLowerCase() === (this.auth.currentUser() || '').toLowerCase(); }
  avatar(url?: string | null): string | null { return this.auth.resolveMediaUrl(url); }
  initials(name?: string | null): string { return (name || '?').trim().charAt(0).toUpperCase() || '?'; }

  relativeTime(value?: string): string {
    if (!value) return '';
    const ms = Date.now() - new Date(value).getTime();
    if (!Number.isFinite(ms)) return '';
    const minutes = Math.floor(ms / 60000);
    if (minutes < 1) return 'now'; if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60); if (hours < 24) return `${hours}h`;
    if (hours < 48) return 'Yesterday'; return new Date(value).toLocaleDateString();
  }

  askDelete(message: DirectMessage): void { if (message.id > 0) this.messageToDelete.set(message); }
  cancelDelete(): void { this.messageToDelete.set(null); }
  confirmDelete(): void {
    const message = this.messageToDelete(); if (!message) return;
    this.messageToDelete.set(null);
    this.service.deleteMessage(message.id).subscribe({ next: () => this.messages.update(rows => rows.filter(row => row.id !== message.id)), error: () => this.error.set('Could not delete this message.') });
  }

  confirmClear(): void {
    const conversation = this.selected(); if (!conversation) return;
    this.clearConfirmation.set(false);
    this.service.clearConversation(conversation.id).subscribe({ next: () => { this.messages.set([]); this.loadConversations(); }, error: () => this.error.set('Could not clear this conversation.') });
  }

  backToList(): void { this.mobileChatOpen.set(false); }
  private scrollToBottom(): void { setTimeout(() => { const element = this.historyElement?.nativeElement; if (element) element.scrollTo({ top: element.scrollHeight, behavior: 'smooth' }); }); }
}
