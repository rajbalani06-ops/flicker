import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { UserProfile, UserService } from '../../core/services/user.service';
import { Post, PageResponse, PostService } from '../../core/services/post.service';
import { AuthService } from '../../core/services/auth.service';
import { of } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, switchMap, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-profile', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './profile.html', styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit, OnDestroy {
  profile = signal<UserProfile | null>(null);
  posts = signal<Post[]>([]);
  error = signal('');
  loading = signal(true);
  saving = signal(false);
  editingBio = signal(false);
  postToDeleteId = signal<number | null>(null);
  deleteError = signal('');
  isDeleting = signal(false);
  bioDraft = '';
  private readonly destroyed = new Subject<void>();

  constructor(private route: ActivatedRoute, private users: UserService, private postService: PostService, public auth: AuthService) { }

  ngOnInit() {
    const usernames = this.route.paramMap.pipe(
      map(params => params.get('username')?.trim() || ''),
      distinctUntilChanged()
    );

    usernames.pipe(
      switchMap(username => {
        this.profile.set(null);
        this.posts.set([]);
        this.error.set('');
        this.loading.set(true);
        if (!username) {
          this.error.set('Could not load this profile.');
          this.loading.set(false);
          return of(null);
        }
        return this.users.getProfile(username).pipe(
          map(result => this.normalizeProfile(result, username)),
          catchError(() => {
            this.error.set('Could not load this profile.');
            this.loading.set(false);
            return of(null);
          })
        );
      }),
      takeUntil(this.destroyed)
    ).subscribe(profile => {
      if (!profile) return;
      this.profile.set(profile);
      if (this.isCurrentUser(profile.username)) this.auth.setAvatarUrl(profile.avatarUrl || null);
      this.loading.set(false);
    });

    usernames.pipe(
      filter(username => !!username),
      switchMap(username => this.users.getProfilePosts(username).pipe(
        catchError(() => of({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0
        })))),
      takeUntil(this.destroyed)
    ).subscribe(response => this.posts.set(response?.content || []));
  }

  ngOnDestroy() { this.destroyed.next(); this.destroyed.complete(); }

  get isOwnProfile() {
    const username = this.profile()?.username;
    return !!username && this.isCurrentUser(username);
  }

  private isCurrentUser(username: string) {
    return username.trim().toLowerCase() === (this.auth.currentUser() || '').trim().toLowerCase();
  }

  private normalizeProfile(profile: UserProfile | null, requestedUsername: string): UserProfile {
    const value = profile || ({} as UserProfile);
    return {
      ...value,
      username: value.username || requestedUsername,
      bio: value.bio || '',
      avatarUrl: this.auth.resolveMediaUrl(value.profileImageUrl || value.avatarUrl) || '',
      followersCount: value.followerCount ?? value.followersCount ?? 0,
      followingCount: value.followingCount ?? 0
    };
  }

  onAvatar(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const currentProfile = this.profile();
    if (!file || !currentProfile || !this.isOwnProfile) return;
    if (!file.type.startsWith('image/')) {
      this.error.set('Choose an image file.');
      input.value = '';
      return;
    }
    this.error.set('');
    this.saving.set(true);
    this.users.updateAvatar(file).subscribe({
      next: result => {
        const updated = this.normalizeProfile({ ...currentProfile, ...result }, currentProfile.username);
        this.profile.set(updated);
        this.auth.setAvatarUrl(updated.avatarUrl || null);
        this.saving.set(false);
        input.value = '';
      },
      error: () => {
        this.error.set('Avatar upload failed.');
        this.saving.set(false);
        input.value = '';
      }
    });
  }

  beginBioEdit() {
    this.bioDraft = this.profile()?.bio || '';
    this.editingBio.set(true);
  }

  cancelBioEdit() { this.editingBio.set(false); }

  saveBio() {
    const profile = this.profile();
    if (!profile || this.saving()) return;
    const bio = this.bioDraft.trim().slice(0, 160);
    this.saving.set(true);
    this.error.set('');
    this.users.updateProfile({ bio }).subscribe({
      next: result => {
        this.profile.set(this.normalizeProfile({ ...profile, ...result, bio: result?.bio ?? bio }, profile.username));
        this.editingBio.set(false);
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Could not save your bio.');
        this.saving.set(false);
      }
    });
  }

  setPrivacy(event: Event) {
    const current = this.profile();
    if (!current) return;
    const isPrivate = (event.target as HTMLInputElement).checked;
    this.saving.set(true);
    this.users.updatePrivacy(isPrivate).subscribe({
      next: result => { this.profile.set(this.normalizeProfile({ ...current, ...result, isPrivate }, current.username)); this.saving.set(false); },
      error: () => { this.error.set('Privacy setting could not be saved.'); this.saving.set(false); }
    });
  }

  deletePost(postId: number) {
    if (!this.isOwnProfile) return;
    this.deleteError.set('');
    this.postToDeleteId.set(postId);
  }

  cancelDelete() {
    if (this.isDeleting()) return;
    this.postToDeleteId.set(null);
    this.deleteError.set('');
  }

  confirmDeletePost() {
    const postId = this.postToDeleteId();
    if (postId === null || this.isDeleting() || !this.isOwnProfile) return;
    this.isDeleting.set(true);
    this.deleteError.set('');
    this.postService.deletePost(postId).subscribe({
      next: () => {
        this.posts.update(current => current.filter(post => post.id !== postId));
        this.postToDeleteId.set(null);
        this.isDeleting.set(false);
      },
      error: (err) => {
        this.deleteError.set(err.error?.message || 'Could not delete this post.');
        this.isDeleting.set(false);
      }
    });
  }
}
