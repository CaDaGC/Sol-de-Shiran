import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-business-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  templateUrl: './business-layout.component.html',
  styleUrl: './business-layout.component.css'
})
export class BusinessLayoutComponent {}
