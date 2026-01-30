const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

const users = [
  {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'john.doe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    displayName: 'John Doe',
    enabled: true,
  },
  {
    id: '00000000-0000-0000-0000-000000000002',
    email: 'jane.roe@example.com',
    firstName: 'Jane',
    lastName: 'Roe',
    displayName: 'Jane Roe',
    enabled: true,
  },
];

// Find all users
app.get('/api/users', (_req, res) => {
  const page = parseInt(_req.query.page || '0', 10);
  const size = parseInt(_req.query.size || '10', 10);
  const start = page * size;
  const content = users.slice(start, start + size);

  res.json({
    content,
    pageable: {
      pageNumber: page,
      pageSize: size,
    },
    totalElements: users.length,
    totalPages: Math.ceil(users.length / size),
    numberOfElements: content.length,
  });
});

// Find user by id
app.get('/api/users/:id', (req, res) => {
  const user = users.find((u) => u.id === req.params.id);

  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  res.json(user);
});

// Create user
app.post('/api/users', (req, res) => {
  const newUser = {
    id: `00000000-0000-0000-0000-${String(users.length + 1).padStart(12, '0')}`,
    ...req.body,
  };

  users.push(newUser);
  res.status(201).json(newUser);
});

// Update user
app.put('/api/users/:id', (req, res) => {
  const index = users.findIndex((u) => u.id === req.params.id);

  if (index === -1) {
    return res.status(404).json({ error: 'User not found' });
  }

  users[index] = { ...users[index], ...req.body };
  res.json(users[index]);
});

// Patch user
app.patch('/api/users/:id', (req, res) => {
  const index = users.findIndex((u) => u.id === req.params.id);

  if (index === -1) {
    return res.status(404).json({ error: 'User not found' });
  }

  users[index] = { ...users[index], ...req.body };
  res.json(users[index]);
});

// Delete user
app.delete('/api/users/:id', (req, res) => {
  const index = users.findIndex((u) => u.id === req.params.id);

  if (index === -1) {
    return res.status(404).json({ error: 'User not found' });
  }

  users.splice(index, 1);
  res.status(204).send();
});

app.listen(PORT, () => {
  console.log(`Mock API running on port ${PORT}`);
});
