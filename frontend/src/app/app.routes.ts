import { Component } from '@angular/core';
import { Routes } from '@angular/router';

@Component({
  template: '',
  styles: ':host { display: none; }',
})
export class RoutePlaceholder {}

export const routes: Routes = [
  { path: '', component: RoutePlaceholder },
  { path: 'produtos', component: RoutePlaceholder },
  { path: 'historico', component: RoutePlaceholder },
  { path: '**', redirectTo: '' },
];
