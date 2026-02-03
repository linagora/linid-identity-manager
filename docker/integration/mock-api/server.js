const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

// Helper to create API error response matching linid-im-api format
function createErrorResponse(status, errorKey, errorContext = {}, details = {}) {
  return {
    error: errorKey, // In real API, this would be i18n translated
    errorKey,
    errorContext,
    status: status,
    timestamp: Date.now(),
    ...details,
  };
}

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
  {
    id: '00000000-0000-0000-0000-000000000003',
    email: 'alice.smith@example.com',
    firstName: 'Alice',
    lastName: 'Smith',
    displayName: 'Alice Smith',
    enabled: true,
  },
  {
    id: '00000000-0000-0000-0000-000000000004',
    email: 'bob.johnson@example.com',
    firstName: 'Bob',
    lastName: 'Johnson',
    displayName: 'Bob Johnson',
    enabled: false,
  },
  {
    id: '00000000-0000-0000-0000-000000000005',
    email: 'charlie.doe@example.com',
    firstName: 'Charlie',
    lastName: 'Doe',
    displayName: 'Charlie Doe',
    enabled: true,
  },
  {
    id: '00000000-0000-0000-0000-000000000006',
    email: 'eva.martin@example.com',
    firstName: 'Eva',
    lastName: 'Martin',
    displayName: 'Eva Martin',
    enabled: true,
  },
];

// Find all users with optional filtering
app.get('/api/users', (req, res) => {
  const page = parseInt(req.query.page || '0', 10);
  const size = parseInt(req.query.size || '10', 10);

  // Filter users based on query parameters
  let filteredUsers = users.filter((user) => {
    if (req.query.email && !user.email.toLowerCase().includes(req.query.email.toLowerCase())) {
      return false;
    }
    if (req.query.firstName && !user.firstName.toLowerCase().includes(req.query.firstName.toLowerCase())) {
      return false;
    }
    if (req.query.lastName && !user.lastName.toLowerCase().includes(req.query.lastName.toLowerCase())) {
      return false;
    }
    return true;
  });

  const start = page * size;
  const content = filteredUsers.slice(start, start + size);
  const totalPages = Math.ceil(filteredUsers.length / size);

  // Return 206 Partial Content when there are more pages
  const statusCode = totalPages > 1 ? 206 : 200;

  res.status(statusCode).json({
    content,
    pageable: {
      pageNumber: page,
      pageSize: size,
    },
    totalElements: filteredUsers.length,
    totalPages,
    numberOfElements: content.length,
  });
});

// Find user by id
app.get('/api/users/:id', (req, res) => {
  const user = users.find((u) => u.id === req.params.id);

  if (!user) {
    return res.status(404).json(createErrorResponse(404, 'error.entity.notFound', { entity: 'user', id: req.params.id }));
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
    return res.status(404).json(createErrorResponse(404, 'error.entity.notFound', { entity: 'user', id: req.params.id }));
  }

  users[index] = { ...users[index], ...req.body };
  res.json(users[index]);
});

// Patch user
app.patch('/api/users/:id', (req, res) => {
  const index = users.findIndex((u) => u.id === req.params.id);

  if (index === -1) {
    return res.status(404).json(createErrorResponse(404, 'error.entity.notFound', { entity: 'user', id: req.params.id }));
  }

  users[index] = { ...users[index], ...req.body };
  res.json(users[index]);
});

// Delete user
app.delete('/api/users/:id', (req, res) => {
  const index = users.findIndex((u) => u.id === req.params.id);

  if (index === -1) {
    return res.status(404).json(createErrorResponse(404, 'error.entity.notFound', { entity: 'user', id: req.params.id }));
  }

  users.splice(index, 1);
  res.status(204).send();
});

app.listen(PORT, () => {
  console.log(`Mock API running on port ${PORT}`);
});
