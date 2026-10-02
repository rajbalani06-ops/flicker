import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Post {
  id: number;
  content: string;
  authorUsername: string;
  authorAvatarUrl?: string;
  createdAt: string;
  imageUrl?: string;
  likeCount?: number;
  likedByCurrentUser?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class PostService {
  private apiUrl = 'http://localhost:8081/api/v1/posts';

  constructor(private http: HttpClient) {}

  getFeed(page = 0, size = 20): Observable<PageResponse<Post>> {
    return this.http.get<PageResponse<Post>>(`${this.apiUrl}/feed?page=${page}&size=${size}`);
  }

  getUserPosts(username: string, page = 0, size = 20): Observable<PageResponse<Post>> {
    return this.http.get<PageResponse<Post>>(`${this.apiUrl}/user/${username}?page=${page}&size=${size}`);
  }

  createPost(content: string, image?: File): Observable<Post> {
    if (!image) return this.http.post<Post>(this.apiUrl, { content });
    const body = new FormData();
    body.append("content", content);
    body.append("image", image);
    return this.http.post<Post>(this.apiUrl, body);
  }

  deletePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${postId}`);
  }

  toggleLike(postId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${postId}/like`, {});
  }
}

