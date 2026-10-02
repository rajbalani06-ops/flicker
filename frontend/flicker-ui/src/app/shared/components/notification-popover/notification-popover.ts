import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-popover',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-popover.html',
  styleUrls: ['./notification-popover.css']
})
export class NotificationPopoverComponent implements OnInit {
  constructor(public notificationService: NotificationService) {}

  ngOnInit() {
    this.notificationService.fetchNotifications().subscribe();
  }
}