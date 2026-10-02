import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

export interface NotificationItem {
  id: number;
  type: string;
  actorUsername: string;
  message: string;
  isRead: boolean;
  targetId: number;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = 'http://localhost:8081/api/v1/notifications';
  
  notifications = signal<NotificationItem[]>([]);
  unreadCount = signal<number>(0);
  loading = signal<boolean>(false);

  constructor(private http: HttpClient) {}

  fetchNotifications() {
    this.loading.set(true);
    return this.http.get<NotificationPage>(this.apiUrl).pipe(
      tap({
        next: (res) => {
          const list = res.content || [];
          this.notifications.set(list);
          this.unreadCount.set(list.filter(n => !n.isRead).length);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }
}