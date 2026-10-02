import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Post, PageResponse } from './post.service';

export interface UserSummary {
  id: number;
  username: string;
  email?: string;
  isFollowing?: boolean;
}

export interface UserProfile {
  id?: number;
  username: string;
  email?: string;
  isFollowing?: boolean;
  bio?: string;
  avatarUrl?: string;
  profileImageUrl?: string;
  createdAt?: string;
  followersCount?: number;
  followerCount?: number;
  followingCount?: number;
  isPrivate?: boolean;
}

export interface UserSearchResult {
  id: number;
  username: string;
  displayName: string;
  profileImageUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = 'http://localhost:8081/api/v1/users';

  constructor(private http: HttpClient) {}

  getSuggestedUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.apiUrl}/suggestions`);
  }

  searchUsers(query: string): Observable<UserSearchResult[]> {
    return this.http.get<UserSearchResult[]>(`${this.apiUrl}/search?q=${encodeURIComponent(query)}`);
  }

  followUser(username: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${username}/follow`, {});
  }

  unfollowUser(username: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${username}/follow`);
  }

  getProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${encodeURIComponent(username)}`);
  }

  getProfilePosts(username: string, page = 0, size = 20): Observable<PageResponse<Post>> {
    return this.http.get<PageResponse<Post>>(`http://localhost:8081/api/v1/posts/user/${encodeURIComponent(username)}?page=${page}&size=${size}`);
  }

  updateAvatar(file: File): Observable<UserProfile> {
    const body = new FormData(); body.append('avatar', file);
    return this.http.post<UserProfile>(`${this.apiUrl}/me/avatar`, body);
  }

  updatePrivacy(isPrivate: boolean): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/me/privacy`, { isPrivate });
  }

  updateProfile(payload: { bio: string }): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/me`, payload);
  }
}
