import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, Subject, Subscription, switchMap } from 'rxjs';
import { UserSearchResult, UserService } from '../../core/services/user.service';
import { PostService, Post } from '../../core/services/post.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-feed',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './feed.html',
    styleUrls: ['./feed.css']
})
export class FeedComponent implements OnInit, OnDestroy {
    posts = signal<Post[]>([]);
    loading = signal(false);
    isPosting = signal(false);
    postSuccess = signal(false);
    newPostContent = '';
    selectedImage: File | null = null;
    imagePreview: string | null = null;
    imageError = '';
    postToDeleteId = signal<number | null>(null);
    deleteError = signal('');
    isDeleting = signal(false);
    private imageInput: HTMLInputElement | null = null;
    searchText = '';
    searchOpen = signal(false);
    searchResults = signal<UserSearchResult[]>([]);
    private searchQuery$ = new Subject<string>();
    private searchSubscription?: Subscription;

    constructor(
        public authService: AuthService,
        private postService: PostService,
        private users: UserService,
        private router: Router
    ) { }

    ngOnInit() {
        this.loadFeed();
        this.searchSubscription = this.searchQuery$.pipe(
            debounceTime(300), distinctUntilChanged(),
            switchMap(query => query.length < 2 ? of([]) : this.users.searchUsers(query).pipe(catchError(() => of([]))))
        ).subscribe(results => this.searchResults.set(results));
    }

    ngOnDestroy() { this.searchSubscription?.unsubscribe(); this.searchQuery$.complete(); }

    searchChanged(value: string) { this.searchText = value; this.searchOpen.set(true); this.searchQuery$.next(value.trim()); }
    openProfile(user: UserSearchResult) { this.searchOpen.set(false); this.router.navigate(['/profile', user.username]); }
    avatarUrl(url?: string) { return this.authService.resolveMediaUrl(url); }

    loadFeed() {
        this.loading.set(true);
        this.postService.getFeed().subscribe({
            next: (res) => {
                this.posts.set(res.content || []);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }

    publishPost() {
        const content = this.newPostContent.trim();
        if ((!content && !this.selectedImage) || this.isPosting()) return;

        this.isPosting.set(true);

        this.postService.createPost(content, this.selectedImage || undefined).subscribe({
            next: (createdPost) => {
                const fallbackPost: Post = createdPost || {
                    id: Date.now(),
                    content: content,
                    authorUsername: this.authService.currentUser() || 'me',
                    createdAt: new Date().toISOString()
                };

                this.posts.update(existing => [fallbackPost, ...existing]);
                this.newPostContent = '';
                this.clearImage();
                this.postSuccess.set(true);

                setTimeout(() => {
                    this.isPosting.set(false);
                    this.postSuccess.set(false);
                }, 1500);
            },
            error: () => {
                this.isPosting.set(false);
            }
        });
    }

    onImageSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        this.imageInput = input;
        const file = input.files?.[0];
        if (!file) return;
        this.imageError = '';
        if (!file.type.startsWith('image/')) { this.imageError = 'Choose an image file.'; input.value = ''; return; }
        if (this.imagePreview) URL.revokeObjectURL(this.imagePreview);
        this.selectedImage = file;
        this.imagePreview = URL.createObjectURL(file);
    }

    clearImage() {
        if (this.imagePreview) URL.revokeObjectURL(this.imagePreview);
        this.imagePreview = null;
        this.selectedImage = null;
        if (this.imageInput) this.imageInput.value = '';
    }


    deletePost(postId: number) {
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
        if (postId === null || this.isDeleting()) return;
        this.isDeleting.set(true);
        this.deleteError.set('');
        this.postService.deletePost(postId).subscribe({
            next: () => {
                this.posts.update(items => items.filter(p => p.id !== postId));
                this.postToDeleteId.set(null);
                this.isDeleting.set(false);
            },
            error: (err) => {
                console.error('Delete failed', err);
                this.deleteError.set(err.error?.message || 'Could not delete this post.');
                this.isDeleting.set(false);
            }
        });
    }

   isAuthor(post: any): boolean {
  const current = (this.authService.currentUser() || '').trim().toLowerCase();
  
  const author = (
    post.authorUsername ||
    post.username ||
    (typeof post.author === 'string' ? post.author : post.author?.username) ||
    ''
  ).trim().toLowerCase();

  

  return !!current && current === author;
}

    likePost(post: Post) {
        this.postService.toggleLike(post.id).subscribe();
    }

    authorAvatarUrl(post: Post) {
        return this.authService.resolveMediaUrl(post.authorAvatarUrl);
    }
}
