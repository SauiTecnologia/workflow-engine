package com.apporte.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Contexto do usuário extraído do JWT do Supabase.
 * Contém informações de autenticação e autorização do usuário.
 */
public class UserContext {
    private String id;
    private String email;
    private String name;
    private List<String> roles;

    /**
     * Construtor padrão para deserialização.
     */
    public UserContext() {
        this.roles = List.of();
    }

    /**
     * Construtor com ID, nome e roles.
     * Usado em testes e inicialização.
     *
     * @param id ID do usuário
     * @param name Nome do usuário
     * @param roles Lista de roles do usuário
     */
    public UserContext(String id, String name, List<String> roles) {
        this.id = Objects.requireNonNull(id, "ID do usuário não pode ser null");
        this.name = Objects.requireNonNull(name, "Nome do usuário não pode ser null");
        this.roles = roles != null ? roles : List.of();
    }

    /**
     * Construtor completo com todos os campos.
     *
     * @param id ID do usuário
     * @param email Email do usuário
     * @param name Nome do usuário
     * @param roles Lista de roles do usuário
     */
    public UserContext(String id, String email, String name, List<String> roles) {
        this.id = Objects.requireNonNull(id, "ID do usuário não pode ser null");
        this.email = email;
        this.name = Objects.requireNonNull(name, "Nome do usuário não pode ser null");
        this.roles = roles != null ? roles : List.of();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? roles : List.of();
    }

    /**
     * Verifica se o usuário tem uma role específica.
     *
     * @param role Role a verificar
     * @return true se o usuário tem a role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Verifica se o usuário tem pelo menos uma das roles fornecidas.
     *
     * @param rolesCheck Lista de roles a verificar
     * @return true se o usuário tem pelo menos uma das roles
     */
    public boolean hasAnyRole(List<String> rolesCheck) {
        return rolesCheck.stream().anyMatch(this::hasRole);
    }

    @Override
    public String toString() {
        return "UserContext{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", roles=" + roles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContext that = (UserContext) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(email, that.email) &&
                Objects.equals(name, that.name) &&
                Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, roles);
    }
}
