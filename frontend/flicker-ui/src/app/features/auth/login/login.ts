import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: [this.route.snapshot.queryParamMap.get('username') || '', [Validators.required]],
      password: ['', [Validators.required]]
    });
    if (this.route.snapshot.queryParamMap.get('registered') === 'true') this.successMessage.set('Account created successfully! Please sign in.');
  }

  onSubmit() {
    if (this.loginForm.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl?.startsWith('/') ? returnUrl : '/feed');
      },
      error: (err) => {
        this.loading.set(false);
        const backendMessage = typeof err.error?.message === 'string' ? err.error.message : null;
        this.errorMessage.set(backendMessage || (err.status === 0
          ? 'Could not connect to Flicker. Check that the backend is running at http://localhost:8081 and try again.'
          : 'Invalid credentials. Please check your username and password.'));
      }
    });
  }
}
