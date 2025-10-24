import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-role-demo',
  templateUrl: './role-demo.component.html',
  styleUrls: ['./role-demo.component.scss']
})
export class RoleDemoComponent implements OnInit {
  currentUser: any = null;
  apiResults: any = {};
  isLoading = false;

  constructor(
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  testEndpoint(endpoint: string) {
    this.isLoading = true;
    const token = this.authService.getToken();
    
    if (!token) {
      this.apiResults[endpoint] = { error: 'No authentication token' };
      this.isLoading = false;
      return;
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    this.http.get(`http://localhost:8081/api/demo/${endpoint}`, { headers })
      .subscribe({
        next: (response) => {
          this.apiResults[endpoint] = { success: response };
          this.isLoading = false;
        },
        error: (error) => {
          this.apiResults[endpoint] = { error: error.error || error.message };
          this.isLoading = false;
        }
      });
  }

  canAccess(role: string): boolean {
    if (!this.currentUser) return false;
    
    switch (role) {
      case 'admin':
        return this.currentUser.role === 'SMA';
      case 'manager':
        return ['SMA', 'MA'].includes(this.currentUser.role);
      case 'user':
        return ['SMA', 'MA', 'MC'].includes(this.currentUser.role);
      case 'support':
        return true; // All authenticated users can access support
      default:
        return false;
    }
  }

  // Helper method to get object keys for template
  getObjectKeys(obj: any): string[] {
    return Object.keys(obj);
  }
}
