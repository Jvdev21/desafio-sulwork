import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home.component').then((module) => module.HomeComponent),
    title: 'Café da Manhã MV',
  },
  {
    path: 'colaboradores',
    loadComponent: () => import('./features/colaboradores/colaborador-list.component').then((module) => module.ColaboradorListComponent),
    title: 'Colaboradores',
  },
  {
    path: 'colaboradores/novo',
    loadComponent: () => import('./features/colaboradores/colaborador-form.component').then((module) => module.ColaboradorFormComponent),
    title: 'Novo colaborador',
  },
  {
    path: 'colaboradores/:id/editar',
    loadComponent: () => import('./features/colaboradores/colaborador-form.component').then((module) => module.ColaboradorFormComponent),
    title: 'Editar colaborador',
  },
  {
    path: 'cafes',
    loadComponent: () => import('./features/cafes/cafe-list.component').then((module) => module.CafeListComponent),
    title: 'Cafés da manhã',
  },
  {
    path: 'cafes/novo',
    loadComponent: () => import('./features/cafes/cafe-form.component').then((module) => module.CafeFormComponent),
    title: 'Novo café',
  },
  {
    path: 'cafes/:id/editar',
    loadComponent: () => import('./features/cafes/cafe-form.component').then((module) => module.CafeFormComponent),
    title: 'Editar café',
  },
  {
    path: 'cafes/:id',
    loadComponent: () => import('./features/cafes/cafe-detail.component').then((module) => module.CafeDetailComponent),
    title: 'Detalhes do café',
  },
  { path: '**', redirectTo: '' },
];
