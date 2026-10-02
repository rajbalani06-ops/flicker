import { Component, signal, ElementRef, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { NotificationPopoverComponent } from '../notification-popover/notification-popover';
import { MessageService } from '../../../core/services/message.service';
import { Subscription, timer } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, NotificationPopoverComponent],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit, OnDestroy {
  isNotificationsOpen = signal(false);
  private unreadSubscription?: Subscription;

  constructor(
    public authService: AuthService,
    public notificationService: NotificationService,
    public messageService: MessageService,
    private router: Router,
    private eRef: ElementRef
  ) {}

  ngOnInit() { this.unreadSubscription = timer(0, 15000).subscribe(() => this.messageService.fetchUnreadCount().subscribe({ error: () => undefined })); }
  ngOnDestroy() { this.unreadSubscription?.unsubscribe(); }

  goToProfile() {
    const user = this.authService.currentUser();
    if (user) {
      this.router.navigate(['/profile', user]);
    }
  }

  toggleNotifications(event: MouseEvent) {
    event.stopPropagation();
    this.isNotificationsOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  clickout(event: MouseEvent) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.isNotificationsOpen.set(false);
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
