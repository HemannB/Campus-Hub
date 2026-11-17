import assert from 'node:assert/strict';
import { before, test } from 'node:test';
import { randomUUID } from 'node:crypto';

const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST;
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
assert.ok(firestoreHost && authHost, 'Execute com scripts/run-rules-tests.sh');
const root = 'projects/demo-campushub/databases/(default)/documents';
const base = 'http://' + firestoreHost + '/v1/' + root;

function field(value) {
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(field) } };
  if (value === null) return { nullValue: null };
  if (typeof value === 'number') return { integerValue: String(value) };
  if (typeof value === 'boolean') return { booleanValue: value };
  return { stringValue: value };
}

function write(path, data, times = [], merge = false) {
  return {
    update: { name: root + '/' + path, fields: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, field(v)])) },
    ...(merge ? { updateMask: { fieldPaths: Object.keys(data) } } : {}),
    ...(times.length ? { updateTransforms: times.map(fieldPath => ({ fieldPath, setToServerValue: 'REQUEST_TIME' })) } : {})
  };
}

async function commit(writes, token = 'owner', allowed = true) {
  const response = await fetch(base + ':commit', {
    method: 'POST', headers: { authorization: 'Bearer ' + token, 'content-type': 'application/json' },
    body: JSON.stringify({ writes })
  });
  const result = await response.json();
  assert.equal(response.ok, allowed, JSON.stringify(result));
  if (!allowed) assert.equal(result.error.status, 'PERMISSION_DENIED');
}

async function user(role) {
  const response = await fetch('http://' + authHost + '/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake', {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email: randomUUID() + '@example.com', password: '123456', returnSecureToken: true })
  });
  const result = await response.json();
  assert.ok(response.ok, JSON.stringify(result));
  const account = { uid: result.localId, token: result.idToken, name: 'Aluno de teste' };
  await commit([write('users/' + account.uid, { id: account.uid, role, name: account.name })]);
  return account;
}

let organizer, otherOrganizer, first, second, third;
before(async () => {
  organizer = await user('ORGANIZER');
  otherOrganizer = await user('ORGANIZER');
  first = await user('STUDENT');
  second = await user('STUDENT');
  third = await user('STUDENT');
});

function event(owner = organizer.uid) {
  return {
    title: 'Oficina', description: 'Kotlin', category: 'Tecnologia', location: 'URI Erechim',
    organizerId: owner, organizer: 'Organizador', imageUrl: '',
    startAt: new Date(Date.now() - 60_000), endAt: new Date(Date.now() + 3_600_000),
    maxParticipants: 1, participantCount: 0, waitingUserIds: [], invitedUserIds: [], status: 'PUBLISHED'
  };
}

const regPath = (account, id) => 'registrations/' + account.uid + '_' + id;
function registration(account, id, status) {
  return write(regPath(account, id), {
    userId: account.uid, userName: account.name, eventId: id, status,
    qrToken: status === 'CONFIRMED' ? randomUUID() : ''
  }, ['registeredAt']);
}
const seats = (id, count, waiting = [], invited = []) => write('events/' + id, {
  participantCount: count, waitingUserIds: waiting, invitedUserIds: invited
}, [], true);

test('aluno não pode mudar o próprio papel nem criar eventos', async () => {
  await commit([write('users/' + first.uid, { role: 'ORGANIZER' }, [], true)], first.token, false);
  const id = randomUUID();
  await commit([write('events/' + id, { ...event(first.uid), status: 'DRAFT' })], first.token, false);
  await commit([write('users/' + first.uid, { name: 'Aluno atualizado' }, [], true)], first.token);
  first.name = 'Aluno atualizado';
});

test('organizador cria, edita, publica e encerra somente o próprio evento', async () => {
  const id = randomUUID();
  await commit([write('events/' + id, { ...event(), status: 'DRAFT' })], organizer.token);
  await commit([write('events/' + id, { title: 'Invasão' }, [], true)], otherOrganizer.token, false);
  await commit([write('events/' + id, { title: 'Oficina atualizada' }, [], true)], organizer.token);
  await commit([write('events/' + id, { status: 'PUBLISHED' }, [], true)], organizer.token);
  await commit([write('events/' + id, { status: 'ENDED' }, ['endAt'], true)], organizer.token);
  await commit([write('events/' + id, { title: 'Não pode mais editar' }, [], true)], organizer.token, false);
});

test('fila reserva a vaga para o primeiro aluno e impede confirmação indevida', async () => {
  const id = randomUUID();
  await commit([write('events/' + id, event())]);
  await commit([registration(first, id, 'CONFIRMED'), seats(id, 1)], first.token);
  await commit([registration(second, id, 'CONFIRMED'), seats(id, 2)], second.token, false);
  await commit([registration(second, id, 'WAITLISTED'), seats(id, 1, [second.uid])], second.token);
  await commit([registration(third, id, 'WAITLISTED'), seats(id, 1, [second.uid, third.uid])], third.token);
  await commit([
    { delete: root + '/' + regPath(first, id) },
    seats(id, 0, [third.uid], [second.uid]),
    write(regPath(second, id), { status: 'INVITED' }, [], true)
  ], first.token);
  await commit([
    write(regPath(third, id), { status: 'CONFIRMED', qrToken: randomUUID() }, [], true),
    seats(id, 1, [], [second.uid])
  ], third.token, false);
  await commit([
    write(regPath(second, id), { status: 'CONFIRMED', qrToken: randomUUID() }, [], true),
    seats(id, 1, [third.uid])
  ], second.token);
  await commit([seats(id, 99)], third.token, false);
});

test('presença só pode ser registrada pelo responsável e uma única vez', async () => {
  const id = randomUUID();
  await commit([write('events/' + id, event())]);
  await commit([registration(first, id, 'CONFIRMED'), seats(id, 1)], first.token);
  const checkIn = () => write(regPath(first, id), {}, ['checkedInAt'], true);
  await commit([checkIn()], first.token, false);
  await commit([checkIn()], otherOrganizer.token, false);
  await commit([checkIn()], organizer.token);
  await commit([checkIn()], organizer.token, false);
  await commit([{ delete: root + '/' + regPath(first, id) }, seats(id, 0)], first.token, false);
});

test('aumento de capacidade oferece a próxima vaga sem permitir furar a fila', async () => {
  const id = randomUUID();
  await commit([write('events/' + id, event())]);
  await commit([registration(first, id, 'CONFIRMED'), seats(id, 1)], first.token);
  await commit([registration(second, id, 'WAITLISTED'), seats(id, 1, [second.uid])], second.token);
  await commit([registration(third, id, 'WAITLISTED'), seats(id, 1, [second.uid, third.uid])], third.token);
  await commit([write('events/' + id, { maxParticipants: 2 }, [], true)], organizer.token);
  await commit([
    seats(id, 1, [second.uid], [third.uid]),
    write(regPath(third, id), { status: 'INVITED' }, [], true)
  ], third.token, false);
  await commit([
    seats(id, 1, [third.uid], [second.uid]),
    write(regPath(second, id), { status: 'INVITED' }, [], true)
  ], second.token);
});
