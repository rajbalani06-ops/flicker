import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { signal } from '@angular/core';

export interface Conversation { id: number; username: string; displayName?: string; avatarUrl?: string; lastMessage?: string; updatedAt?: string; unreadCount?: number; }
export interface DirectMessage { id: number; senderUsername: string; content: string; createdAt: string; }

@Injectable({ providedIn: 'root' })
export class MessageService {
  private api = 'http://localhost:8081/api/v1/messages';
  unreadCount = signal(0);
  constructor(private http: HttpClient) {}
  conversations(): Observable<Conversation[]> { return this.http.get<Conversation[]>(this.api + '/conversations'); }
  fetchUnreadCount(): Observable<{ count: number }> { return this.http.get<{ count: number }>(this.api + '/unread-count').pipe(tap(result => this.unreadCount.set(result.count || 0))); }
  markRead(id: number): Observable<void> { return this.http.patch<void>(this.api + '/conversations/' + id + '/read', {}); }
  decrementUnread(count: number): void { this.unreadCount.update(total => Math.max(0, total - count)); }
  startConversation(username: string): Observable<Conversation> { return this.http.post<Conversation>(this.api + '/conversations/with/' + encodeURIComponent(username), {}); }
  history(conversationId: number): Observable<DirectMessage[]> { return this.http.get<DirectMessage[]>(this.api + '/conversations/' + conversationId); }
  send(conversationId: number, content: string): Observable<DirectMessage> { return this.http.post<DirectMessage>(this.api + '/conversations/' + conversationId, { content }); }
  deleteMessage(id: number): Observable<void> { return this.http.delete<void>(this.api + '/' + id); }
  clearConversation(id: number): Observable<void> { return this.http.delete<void>(this.api + '/conversations/' + id); }
}
