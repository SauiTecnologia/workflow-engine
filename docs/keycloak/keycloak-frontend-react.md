# Integração Frontend React com Keycloak

## 🎯 Visão Geral

Este guia mostra como integrar uma aplicação React com o Keycloak para autenticação e autorização.

## 📦 Instalação

### Opção 1: keycloak-js (Recomendado)

```bash
npm install keycloak-js
# ou
yarn add keycloak-js
```

### Opção 2: @react-keycloak/web (React Context API)

```bash
npm install @react-keycloak/web keycloak-js
# ou
yarn add @react-keycloak/web keycloak-js
```

---

## 🔧 Configuração Básica

### 1. Criar arquivo de configuração do Keycloak

Crie `src/config/keycloak.ts`:

```typescript
import Keycloak from 'keycloak-js';

// Configuração para Development
const keycloakConfig = {
  url: 'https://auth.apporte.work',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'development',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'apporte-frontend-dev',
};

// Inicializar Keycloak
const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
```

### 2. Criar arquivo .env

Crie `.env` na raiz do projeto:

```bash
# Development
REACT_APP_KEYCLOAK_REALM=development
REACT_APP_KEYCLOAK_CLIENT_ID=apporte-frontend-dev
REACT_APP_API_URL=http://localhost:8081

# Production (descomentar quando fizer deploy)
# REACT_APP_KEYCLOAK_REALM=production
# REACT_APP_KEYCLOAK_CLIENT_ID=apporte-frontend-prod
# REACT_APP_API_URL=https://api.apporte.work
```

---

## 🚀 Implementação

### Opção A: Usando keycloak-js diretamente

#### 1. Inicializar Keycloak no App.tsx

```typescript
// src/App.tsx
import React, { useEffect, useState } from 'react';
import keycloak from './config/keycloak';

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Inicializar Keycloak
    keycloak.init({
      onLoad: 'check-sso', // ou 'login-required' para forçar login
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      checkLoginIframe: false, // Replit pode ter problemas com iframe
      pkceMethod: 'S256', // Usar PKCE para segurança
    })
    .then((auth) => {
      setAuthenticated(auth);
      setLoading(false);

      if (auth) {
        console.log('Usuário autenticado:', keycloak.tokenParsed);
        
        // Auto-refresh do token
        setInterval(() => {
          keycloak.updateToken(70).catch(() => {
            console.error('Failed to refresh token');
            keycloak.logout();
          });
        }, 60000); // A cada 1 minuto
      }
    })
    .catch((error) => {
      console.error('Keycloak initialization error:', error);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return <div>Carregando...</div>;
  }

  if (!authenticated) {
    return (
      <div>
        <h1>Bem-vindo ao Apporte</h1>
        <button onClick={() => keycloak.login()}>
          Fazer Login
        </button>
      </div>
    );
  }

  return (
    <div>
      <h1>Olá, {keycloak.tokenParsed?.name}!</h1>
      <button onClick={() => keycloak.logout()}>
        Sair
      </button>
      {/* Resto da sua aplicação */}
    </div>
  );
}

export default App;
```

#### 2. Criar Context para Keycloak (Recomendado)

```typescript
// src/contexts/AuthContext.tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import keycloak from '../config/keycloak';

interface UserInfo {
  id: string;
  email: string;
  name: string;
  firstName: string;
  lastName: string;
  username: string;
  organizationId?: string;
  organizationName?: string;
  roles: string[];
  groups: string[];
}

interface AuthContextType {
  authenticated: boolean;
  loading: boolean;
  userInfo: UserInfo | null;
  token: string | null;
  login: () => void;
  logout: () => void;
  hasRole: (role: string) => boolean;
  isSystemAdmin: () => boolean;
  isOrgAdmin: () => boolean;
  isProponente: () => boolean;
  isParecerista: () => boolean;
  isInvestidor: () => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

  useEffect(() => {
    keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      checkLoginIframe: false,
      pkceMethod: 'S256',
    })
    .then((auth) => {
      setAuthenticated(auth);
      
      if (auth && keycloak.tokenParsed) {
        const parsed = keycloak.tokenParsed as any;
        
        setUserInfo({
          id: parsed.sub,
          email: parsed.email,
          name: parsed.name,
          firstName: parsed.given_name,
          lastName: parsed.family_name,
          username: parsed.preferred_username,
          organizationId: parsed.org_id,
          organizationName: parsed.org_name,
          roles: parsed.realm_access?.roles || [],
          groups: parsed.groups || [],
        });

        // Auto-refresh token
        setInterval(() => {
          keycloak.updateToken(70).catch(() => {
            console.error('Failed to refresh token');
            logout();
          });
        }, 60000);
      }
      
      setLoading(false);
    })
    .catch((error) => {
      console.error('Keycloak initialization error:', error);
      setLoading(false);
    });
  }, []);

  const login = () => {
    keycloak.login();
  };

  const logout = () => {
    keycloak.logout();
  };

  const hasRole = (role: string): boolean => {
    return keycloak.hasRealmRole(role);
  };

  const isSystemAdmin = () => hasRole('system-admin');
  const isOrgAdmin = () => hasRole('org-admin');
  const isProponente = () => hasRole('proponente');
  const isParecerista = () => hasRole('parecerista');
  const isInvestidor = () => hasRole('investidor');

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        loading,
        userInfo,
        token: keycloak.token || null,
        login,
        logout,
        hasRole,
        isSystemAdmin,
        isOrgAdmin,
        isProponente,
        isParecerista,
        isInvestidor,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

#### 3. Usar o Context no App

```typescript
// src/App.tsx
import React from 'react';
import { AuthProvider } from './contexts/AuthContext';
import MainApp from './components/MainApp';

function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

export default App;
```

```typescript
// src/components/MainApp.tsx
import React from 'react';
import { useAuth } from '../contexts/AuthContext';

const MainApp: React.FC = () => {
  const { authenticated, loading, userInfo, login, logout } = useAuth();

  if (loading) {
    return (
      <div className="loading-container">
        <h2>Carregando...</h2>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="login-container">
        <h1>Bem-vindo ao Apporte 2.0</h1>
        <p>Sistema de gestão de propostas e investimentos</p>
        <button onClick={login} className="btn-login">
          Fazer Login
        </button>
      </div>
    );
  }

  return (
    <div className="app-container">
      <header>
        <h1>Apporte 2.0</h1>
        <div className="user-info">
          <span>Olá, {userInfo?.name}!</span>
          {userInfo?.organizationName && (
            <span className="org-badge">{userInfo.organizationName}</span>
          )}
          <button onClick={logout}>Sair</button>
        </div>
      </header>

      <main>
        {/* Sua aplicação aqui */}
        <Dashboard />
      </main>
    </div>
  );
};

export default MainApp;
```

---

## 🔒 Componentes de Proteção de Rotas

### ProtectedRoute Component

```typescript
// src/components/ProtectedRoute.tsx
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  requireOrganization?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRoles,
  requireOrganization,
}) => {
  const { authenticated, loading, userInfo, hasRole } = useAuth();

  if (loading) {
    return <div>Carregando...</div>;
  }

  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  // Verificar se possui role necessária
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => hasRole(role));
    if (!hasRequiredRole) {
      return (
        <div className="access-denied">
          <h2>Acesso Negado</h2>
          <p>Você não tem permissão para acessar esta página.</p>
        </div>
      );
    }
  }

  // Verificar se usuário pertence a uma organização
  if (requireOrganization && !userInfo?.organizationId) {
    return (
      <div className="no-organization">
        <h2>Organização Necessária</h2>
        <p>Você precisa estar vinculado a uma organização para acessar esta página.</p>
      </div>
    );
  }

  return <>{children}</>;
};

export default ProtectedRoute;
```

### Uso com React Router

```typescript
// src/App.tsx com rotas
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import Dashboard from './pages/Dashboard';
import Propostas from './pages/Propostas';
import AdminPanel from './pages/AdminPanel';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Rota pública */}
          <Route path="/" element={<Home />} />

          {/* Rotas protegidas */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          {/* Rota apenas para proponentes */}
          <Route
            path="/propostas"
            element={
              <ProtectedRoute
                requiredRoles={['proponente', 'org-admin', 'system-admin']}
                requireOrganization
              >
                <Propostas />
              </ProtectedRoute>
            }
          />

          {/* Rota apenas para admins */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRoles={['system-admin']}>
                <AdminPanel />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
```

---

## 🌐 Chamadas à API com Token

### Criar cliente HTTP com interceptor

```typescript
// src/services/api.ts
import axios from 'axios';
import keycloak from '../config/keycloak';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8081',
});

// Interceptor para adicionar token automaticamente
api.interceptors.request.use(
  async (config) => {
    if (keycloak.token) {
      // Atualizar token se necessário
      try {
        await keycloak.updateToken(30);
      } catch (error) {
        console.error('Failed to refresh token', error);
        keycloak.logout();
      }

      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }

    // Adicionar org_id se existir
    const orgId = (keycloak.tokenParsed as any)?.org_id;
    if (orgId) {
      config.headers['X-Organization-ID'] = orgId;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para tratamento de erros
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error('Unauthorized - redirecting to login');
      keycloak.logout();
    }
    return Promise.reject(error);
  }
);

export default api;
```

### Usar o cliente API

```typescript
// src/services/propostas.service.ts
import api from './api';

export interface Proposta {
  id: string;
  titulo: string;
  descricao: string;
  status: string;
  criadoPor: string;
  organizationId: string;
}

export const propostasService = {
  // Listar propostas
  async listar(): Promise<Proposta[]> {
    const response = await api.get('/api/propostas');
    return response.data;
  },

  // Criar proposta
  async criar(proposta: Omit<Proposta, 'id'>): Promise<Proposta> {
    const response = await api.post('/api/propostas', proposta);
    return response.data;
  },

  // Buscar por ID
  async buscarPorId(id: string): Promise<Proposta> {
    const response = await api.get(`/api/propostas/${id}`);
    return response.data;
  },

  // Atualizar proposta
  async atualizar(id: string, proposta: Partial<Proposta>): Promise<Proposta> {
    const response = await api.put(`/api/propostas/${id}`, proposta);
    return response.data;
  },

  // Deletar proposta
  async deletar(id: string): Promise<void> {
    await api.delete(`/api/propostas/${id}`);
  },
};
```

---

## 🎨 Componentes de UI Condicionais

### Mostrar conteúdo baseado em role

```typescript
// src/components/RoleBasedComponent.tsx
import React from 'react';
import { useAuth } from '../contexts/AuthContext';

interface RoleBasedComponentProps {
  children: React.ReactNode;
  requiredRoles: string[];
  fallback?: React.ReactNode;
}

const RoleBasedComponent: React.FC<RoleBasedComponentProps> = ({
  children,
  requiredRoles,
  fallback = null,
}) => {
  const { hasRole } = useAuth();

  const hasRequiredRole = requiredRoles.some(role => hasRole(role));

  if (!hasRequiredRole) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

export default RoleBasedComponent;
```

### Uso em componentes

```typescript
// src/pages/Dashboard.tsx
import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import RoleBasedComponent from '../components/RoleBasedComponent';

const Dashboard: React.FC = () => {
  const { userInfo } = useAuth();

  return (
    <div className="dashboard">
      <h1>Dashboard - {userInfo?.organizationName}</h1>

      {/* Botão visível apenas para proponentes */}
      <RoleBasedComponent requiredRoles={['proponente', 'org-admin']}>
        <button className="btn-primary">
          Criar Nova Proposta
        </button>
      </RoleBasedComponent>

      {/* Seção visível apenas para pareceristas */}
      <RoleBasedComponent requiredRoles={['parecerista', 'org-admin']}>
        <section className="avaliacoes-pendentes">
          <h2>Avaliações Pendentes</h2>
          {/* Lista de propostas para avaliar */}
        </section>
      </RoleBasedComponent>

      {/* Seção visível apenas para investidores */}
      <RoleBasedComponent requiredRoles={['investidor', 'org-admin']}>
        <section className="oportunidades-investimento">
          <h2>Oportunidades de Investimento</h2>
          {/* Lista de propostas aprovadas */}
        </section>
      </RoleBasedComponent>

      {/* Painel admin visível apenas para admins do sistema */}
      <RoleBasedComponent requiredRoles={['system-admin']}>
        <section className="admin-panel">
          <h2>Administração do Sistema</h2>
          <button>Gerenciar Usuários</button>
          <button>Gerenciar Organizações</button>
        </section>
      </RoleBasedComponent>
    </div>
  );
};

export default Dashboard;
```

---

## 🧪 Testes

### Mock do Keycloak para testes

```typescript
// src/__mocks__/keycloak.ts
const keycloakMock = {
  init: jest.fn().mockResolvedValue(true),
  login: jest.fn(),
  logout: jest.fn(),
  updateToken: jest.fn().mockResolvedValue(true),
  hasRealmRole: jest.fn(),
  token: 'mock-token',
  tokenParsed: {
    sub: 'user-123',
    email: 'test@example.com',
    name: 'Test User',
    given_name: 'Test',
    family_name: 'User',
    preferred_username: 'testuser',
    org_id: 'org-test-001',
    org_name: 'Test Organization',
    realm_access: {
      roles: ['proponente'],
    },
    groups: ['org-test'],
  },
};

export default keycloakMock;
```

---

## 📱 Arquivo silent-check-sso.html

Crie na pasta `public/`:

```html
<!-- public/silent-check-sso.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Silent SSO Check</title>
</head>
<body>
    <script>
        parent.postMessage(location.href, location.origin);
    </script>
</body>
</html>
```

---

## ✅ Checklist de Implementação

- [ ] Instalar `keycloak-js`
- [ ] Criar `src/config/keycloak.ts`
- [ ] Criar `.env` com configurações
- [ ] Criar `AuthContext` com hook `useAuth`
- [ ] Envolver App com `<AuthProvider>`
- [ ] Criar componente `ProtectedRoute`
- [ ] Criar cliente API com interceptors
- [ ] Criar `public/silent-check-sso.html`
- [ ] Testar login/logout
- [ ] Testar chamadas à API com token
- [ ] Testar proteção de rotas por role

---

## 🐛 Troubleshooting

### Erro: "CORS policy" ao fazer login

**Solução**: Adicione a URL do Replit nas "Valid redirect URIs" do client no Keycloak.

### Token não é enviado nas requisições

**Solução**: Verifique se o interceptor está configurado corretamente no axios.

### Login funciona mas API retorna 401

**Solução**: 
1. Verifique se o backend está usando o mesmo realm
2. Decodifique o token em jwt.io e verifique as claims
3. Verifique logs do backend

### iframe errors no Replit

**Solução**: Use `checkLoginIframe: false` na configuração do init().

---

**🎉 Frontend React integrado com Keycloak!**
