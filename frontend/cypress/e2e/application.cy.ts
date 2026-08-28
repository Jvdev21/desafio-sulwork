function digits(value: number, size: number): string {
  return String(value).replace(/\D/g, '').padStart(size, '0').slice(-size);
}

function validCpf(seed: number): string {
  let base = digits(seed, 9);
  if (/^(\d)\1+$/.test(base)) base = `12345${base.slice(-4)}`;
  const check = (source: string, weight: number) => {
    const sum = [...source].reduce((total, digit, index) => total + Number(digit) * (weight - index), 0);
    const remainder = (sum * 10) % 11;
    return remainder === 10 ? 0 : remainder;
  };
  const first = check(base, 10);
  return `${base}${first}${check(`${base}${first}`, 11)}`;
}

function futureIso(offset: number): string {
  const date = new Date();
  date.setHours(12, 0, 0, 0);
  date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function todayIso(): string { return futureIso(0); }

function selectFutureDate(offset: number): void {
  const today = new Date();
  today.setHours(12, 0, 0, 0);
  const target = new Date(today);
  target.setDate(target.getDate() + offset);
  const monthAdvances = (target.getFullYear() - today.getFullYear()) * 12 + target.getMonth() - today.getMonth();

  cy.get('mat-datepicker-toggle button').click();
  for (let index = 0; index < monthAdvances; index += 1) {
    cy.get('.mat-calendar-next-button').click();
  }
  cy.contains('button.mat-calendar-body-cell', new RegExp(`^\\s*${target.getDate()}\\s*$`)).click();
}

describe('Desafio Sulwork — fluxos principais', () => {
  let cafeIds: number[];
  let collaboratorIds: number[];
  const runSeed = Date.now() % 900_000_000;

  beforeEach(() => {
    cafeIds = [];
    collaboratorIds = [];
  });

  afterEach(() => {
    [...cafeIds].reverse().forEach((id) => cy.request({ method: 'DELETE', url: `/api/cafes/${id}`, failOnStatusCode: false }));
    [...collaboratorIds].reverse().forEach((id) => cy.request({ method: 'DELETE', url: `/api/colaboradores/${id}`, failOnStatusCode: false }));
  });

  it('abre a página inicial e navega pelos acessos principais', () => {
    cy.visit('/');
    cy.get('main').contains('h1', 'Café da Manhã MV').should('be.visible');
    cy.get('[data-cy="home-colaboradores"]').click();
    cy.location('pathname').should('equal', '/colaboradores');
    cy.get('nav').contains('a', 'Cafés da manhã').click();
    cy.location('pathname').should('equal', '/cafes');
    cy.get('[data-cy="nav-inicio"]').click();
    cy.location('pathname').should('equal', '/');
  });

  it('cadastra e edita um colaborador', () => {
    const cpf = validCpf(runSeed + 1);
    cy.intercept('POST', '/api/colaboradores').as('createCollaborator');
    cy.visit('/colaboradores/novo');
    cy.get('[formControlName="nome"]').type('Colaborador Cypress');
    cy.get('[formControlName="cpf"]').type(cpf);
    cy.contains('button', 'Salvar').click();
    cy.wait('@createCollaborator').then(({ response }) => collaboratorIds.push(response!.body.id));
    cy.intercept('GET', '/api/colaboradores/*').as('loadCollaborator');
    cy.contains('tr', 'Colaborador Cypress').within(() => cy.get('[aria-label="Editar"]').click());
    cy.wait('@loadCollaborator');
    cy.intercept('PUT', '/api/colaboradores/*').as('updateCollaborator');
    cy.get('[formControlName="nome"]').clear().type('Colaborador Cypress Editado');
    cy.contains('button', 'Salvar').click();
    cy.wait('@updateCollaborator').its('request.body.nome').should('equal', 'Colaborador Cypress Editado');
    cy.contains('tr', 'Colaborador Cypress Editado').should('be.visible');
  });

  it('rejeita CPF inválido antes de enviar', () => {
    cy.intercept('POST', '/api/colaboradores').as('createCollaborator');
    cy.visit('/colaboradores/novo');
    cy.get('[formControlName="nome"]').type('CPF Inválido');
    cy.get('[formControlName="cpf"]').type('11111111111');
    cy.contains('button', 'Salvar').click();
    cy.contains('CPF inválido.').should('be.visible');
    cy.get('@createCollaborator.all').should('have.length', 0);
  });

  it('mostra o conflito de CPF duplicado retornado pela API', () => {
    const cpf = validCpf(runSeed + 2);
    cy.request('POST', '/api/colaboradores', { nome: 'CPF Original Cypress', cpf }).then(({ body }) => collaboratorIds.push(body.id));
    cy.visit('/colaboradores/novo');
    cy.get('[formControlName="nome"]').type('CPF Duplicado Cypress');
    cy.get('[formControlName="cpf"]').type(cpf);
    cy.contains('button', 'Salvar').click();
    cy.contains('CPF já cadastrado.').should('be.visible');
  });

  it('valida a data obrigatória e cria um café futuro', () => {
    cy.visit('/cafes/novo');
    cy.contains('button', 'Criar café').click();
    cy.contains('Data é obrigatória.').should('be.visible');

    cy.intercept('POST', '/api/cafes').as('createCafe');
    selectFutureDate(40 + (runSeed % 20));
    cy.contains('button', 'Criar café').click();
    cy.wait('@createCafe').then(({ response }) => {
      cafeIds.push(response!.body.id);
      cy.location('pathname').should('equal', `/cafes/${response!.body.id}`);
    });
    cy.contains('mat-chip', 'PRÓXIMO').should('be.visible');
  });

  it('mantém múltiplos itens na mesma participação e atualiza a disponibilidade', () => {
    const cpfOne = validCpf(runSeed + 3);
    const cpfTwo = validCpf(runSeed + 4);
    cy.intercept('POST', '/api/colaboradores').as('createParticipantCollaborator');
    cy.visit('/colaboradores/novo');
    cy.get('[formControlName="nome"]').type('Participante Um Cypress');
    cy.get('[formControlName="cpf"]').type(cpfOne);
    cy.contains('button', 'Salvar').click();
    cy.wait('@createParticipantCollaborator').then(({ response }) => collaboratorIds.push(response!.body.id));
    cy.request('POST', '/api/colaboradores', { nome: 'Participante Dois Cypress', cpf: cpfTwo }).then(({ body }) => collaboratorIds.push(body.id));
    cy.intercept('POST', '/api/cafes').as('createCafeForParticipation');
    cy.visit('/cafes/novo');
    selectFutureDate(70 + (runSeed % 20));
    cy.contains('button', 'Criar café').click();
    cy.wait('@createCafeForParticipation').then(({ response }) => {
      cafeIds.push(response!.body.id);
      cy.location('pathname').should('equal', `/cafes/${response!.body.id}`);
    });
    cy.contains('Nenhum participante adicionado ainda.').should('be.visible');

    cy.contains('button', 'Adicionar primeiro participante').click();
    cy.contains('h2', 'Adicionar participante ao café').should('be.visible');
    cy.get('mat-select[formControlName="colaboradorId"]').click();
    cy.contains('mat-option', 'Participante Um Cypress').click();
    cy.get('mat-dialog-container').should('contain.text', 'Participante Um Cypress').and('contain.text', 'CPF');
    cy.get('[data-cy="contador-itens"]').should('contain.text', '0 itens escolhidos').and('not.contain.text', ' de ');
    cy.get('[data-cy="confirmar-participacao"]').should('be.disabled');
    cy.get('[data-cy="item-cafe"]').click();
    cy.get('[data-cy="contador-itens"]').should('contain.text', '1 item escolhido');
    cy.get('[data-cy="item-agua"]').click();
    cy.get('[data-cy="item-pao"]').click();
    cy.get('[data-cy="item-cafe"]').should('have.class', 'selected');
    cy.get('[data-cy="item-agua"]').should('have.class', 'selected');
    cy.get('[data-cy="item-pao"]').should('have.class', 'selected');
    cy.get('[data-cy="item-bolo"]').click();
    cy.get('[data-cy="contador-itens"]').should('contain.text', '4 itens escolhidos').and('not.contain.text', ' de ');
    cy.get('[data-cy="item-cafe"]').should('have.class', 'selected');
    cy.get('[data-cy="item-agua"]').should('have.class', 'selected');
    cy.get('[data-cy="item-pao"]').should('have.class', 'selected');
    cy.get('[data-cy="item-bolo"]').should('have.class', 'selected');
    cy.get('[data-cy="item-suco"]').should('not.be.disabled');

    cy.intercept('POST', '/api/cafes/*/participantes').as('confirmParticipation');
    cy.get('[data-cy="confirmar-participacao"]').click();
    cy.wait('@confirmParticipation').then(({ request }) => {
      expect(request.body.itens).to.deep.equal(['Café', 'Água', 'Pão', 'Bolo']);
    });
    cy.contains('Participação confirmada com sucesso.').should('be.visible');
    cy.get('mat-card.participant-row').filter(':contains("Participante Um Cypress")').should('have.length', 1)
      .and('contain.text', 'Café').and('contain.text', 'Água').and('contain.text', 'Pão').and('contain.text', 'Bolo');

    cy.contains('button', 'Adicionar participante').click();
    cy.get('mat-select[formControlName="colaboradorId"]').click();
    cy.contains('mat-option', 'Participante Dois Cypress').click();
    cy.get('[data-cy="item-cafe"]').should('be.disabled').and('contain.text', 'Já escolhido neste café');
    cy.get('[data-cy="item-agua"]').should('be.disabled').and('contain.text', 'Já escolhido neste café');
    cy.get('[data-cy="item-pao"]').should('be.disabled').and('contain.text', 'Já escolhido neste café');
    cy.get('[data-cy="item-bolo"]').should('be.disabled').and('contain.text', 'Já escolhido neste café');
    cy.get('[data-cy="item-suco"]').should('not.be.disabled').click();
    cy.get('[data-cy="item-frutas"]').should('not.be.disabled').click();
    cy.get('[data-cy="contador-itens"]').should('contain.text', '2 itens escolhidos').and('not.contain.text', ' de ');
    cy.get('[data-cy="confirmar-participacao"]').should('not.be.disabled').click();
    cy.wait('@confirmParticipation').its('request.body.itens').should('deep.equal', ['Suco', 'Frutas']);
    cy.contains('Participação confirmada com sucesso.').should('be.visible');
    cy.get('mat-card.participant-row').should('have.length', 2);
    cy.get('mat-card.participant-row').filter(':contains("Participante Um Cypress")').should('have.length', 1);
    cy.get('mat-card.participant-row').filter(':contains("Participante Dois Cypress")').should('have.length', 1)
      .and('contain.text', 'Suco').and('contain.text', 'Frutas');
  });

  it('altera status no dia do café sem oferecer PENDENTE', () => {
    const item = { id: 991, participacaoId: 990, cafeId: 989, nome: 'Bolo do dia', status: 'PENDENTE', createdAt: '', updatedAt: '' };
    cy.intercept('GET', '/api/cafes/989', { id: 989, data: todayIso(), createdAt: '' });
    cy.intercept('GET', '/api/colaboradores', [{ id: 988, nome: 'Pessoa do Dia', cpf: '52998224725', createdAt: '', updatedAt: '' }]);
    cy.intercept('GET', '/api/cafes/989/participantes', [{ id: 990, cafeId: 989, colaboradorId: 988, createdAt: '', itens: [item] }]);
    cy.intercept('PATCH', '/api/itens/991/status', (request) => {
      expect(request.body.status).to.be.oneOf(['TROUXE', 'NAO_TROUXE']);
      request.reply({ ...item, status: request.body.status });
    }).as('changeStatus');

    cy.visit('/cafes/989');
    cy.contains('mat-chip', 'HOJE').should('be.visible');
    cy.get('[aria-label="Marcar como trouxe"]').click();
    cy.wait('@changeStatus').its('request.body').should('deep.equal', { status: 'TROUXE' });
    cy.contains('mat-chip', 'Trouxe').should('be.visible');
    cy.get('[aria-label="Marcar como não trouxe"]').click();
    cy.wait('@changeStatus').its('request.body').should('deep.equal', { status: 'NAO_TROUXE' });
    cy.contains('mat-chip', 'Não trouxe').should('be.visible');
    cy.contains('button', 'Pendente').should('not.exist');
  });
});
