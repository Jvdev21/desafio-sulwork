import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideRouter([]), provideNoopAnimations()],
    }).compileComponents();
  });

  it('exibe a apresentação e os acessos principais', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Café da Manhã MV');
    expect(element.textContent).toContain('Organize os cafés da equipe com praticidade e sem conflitos.');
    expect(element.querySelector('[data-cy="home-colaboradores"]')?.getAttribute('href')).toBe('/colaboradores');
    expect(element.querySelector('[data-cy="home-cafes"]')?.getAttribute('href')).toBe('/cafes');
  });
});
