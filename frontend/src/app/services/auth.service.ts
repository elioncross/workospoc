import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8081/api';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    // Check for existing token on service initialization
    this.checkAuthStatus();
  }

  login(username: string, password: string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    const body = {
      username: username,
      password: password
    };

    return this.http.post(`${this.API_URL}/auth/login`, body, { headers })
      .pipe(
        tap((response: any) => {
          // Store the JWT token
          if (response.token) {
            this.setToken(response.token);
            // Update current user with the response data
            this.currentUserSubject.next({
              username: response.username,
              corpId: response.corpId,
              role: response.role,
              authenticated: true
            });
          }
        })
      );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
    // Automatically fetch user details when token is set
    this.fetchCurrentUser();
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token;
  }

  fetchCurrentUser(): void {
    const token = this.getToken();
    if (token) {
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      this.http.get<User>(`${this.API_URL}/me`, { headers })
        .subscribe({
          next: (user) => {
            this.currentUserSubject.next(user);
          },
          error: (error) => {
            console.error('Error fetching user:', error);
            this.logout();
          }
        });
    }
  }

  private checkAuthStatus(): void {
    if (this.isAuthenticated()) {
      this.fetchCurrentUser();
    }
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user?.role === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.currentUserSubject.value;
    return user ? roles.includes(user.role) : false;
  }
}
