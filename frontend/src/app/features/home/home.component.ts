import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-home',
  imports: [RouterLink, MatButton, MatCard, MatIcon],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  readonly passos = [
    { numero: '01', icone: 'person_add', titulo: 'Cadastre colaboradores', texto: 'Inclua as pessoas que farão parte dos encontros.' },
    { numero: '02', icone: 'event_available', titulo: 'Crie um café', texto: 'Escolha uma data futura para o café da manhã.' },
    { numero: '03', icone: 'groups', titulo: 'Adicione participantes', texto: 'Monte o grupo que participará de cada encontro.' },
    { numero: '04', icone: 'bakery_dining', titulo: 'Informe os itens', texto: 'Registre o que cada pessoa se comprometeu a levar.' },
    { numero: '05', icone: 'check_circle', titulo: 'Acompanhe as entregas', texto: 'No dia, acompanhe o status de cada contribuição.' },
  ];
}
