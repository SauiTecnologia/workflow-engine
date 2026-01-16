#!/bin/bash
set -e

# ========================================
# GitHub Secrets Setup Script
# ========================================
# Automatically configure GitHub secrets from .envrc
# Requires: gh (GitHub CLI)

echo "🔐 GitHub Secrets Setup"
echo ""

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) not found!"
    echo ""
    echo "Install it:"
    echo "  # Ubuntu/Debian"
    echo "  sudo apt install gh"
    echo ""
    echo "  # macOS"
    echo "  brew install gh"
    echo ""
    echo "  # Or: https://cli.github.com/"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub"
    echo ""
    echo "Run: gh auth login"
    exit 1
fi

echo "✅ GitHub CLI authenticated"
echo ""

# Check if .envrc exists
if [ ! -f ".envrc" ]; then
    echo "❌ Error: .envrc file not found!"
    echo "Create .envrc from .envrc.example first."
    exit 1
fi

# Source environment variables
echo "📋 Loading secrets from .envrc..."
source .envrc

# Validate required secrets
if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD not set in .envrc"
    exit 1
fi

# Support both OIDC_CLIENT_SECRET and KEYCLOAK_CLIENT_SECRET
KEYCLOAK_SECRET="${KEYCLOAK_CLIENT_SECRET:-$OIDC_CLIENT_SECRET}"
if [ -z "$KEYCLOAK_SECRET" ]; then
    echo "❌ Error: KEYCLOAK_CLIENT_SECRET not set in .envrc"
    exit 1
fi

echo "✅ Secrets loaded from .envrc"
echo ""

# Get current repository
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null)
if [ -z "$REPO" ]; then
    echo "❌ Error: Not in a GitHub repository"
    echo "Make sure you're in the workflow-engine directory"
    exit 1
fi

echo "📦 Repository: $REPO"
echo ""

# Ask for DigitalOcean token
echo "🔑 DigitalOcean Access Token"
echo "Get it from: https://cloud.digitalocean.com/account/api/tokens"
echo ""
read -sp "Enter DIGITALOCEAN_ACCESS_TOKEN (paste here): " DO_TOKEN
echo ""

if [ -z "$DO_TOKEN" ]; then
    echo "❌ Error: DigitalOcean token is required"
    exit 1
fi

echo ""
echo "🚀 Setting GitHub secrets..."
echo ""

# Set secrets
echo "Setting DB_PASSWORD..."
echo "$DB_PASSWORD" | gh secret set DB_PASSWORD

echo "Setting KEYCLOAK_CLIENT_SECRET..."
echo "$KEYCLOAK_SECRET" | gh secret set KEYCLOAK_CLIENT_SECRET

echo "Setting DIGITALOCEAN_ACCESS_TOKEN..."
echo "$DO_TOKEN" | gh secret set DIGITALOCEAN_ACCESS_TOKEN

echo ""
echo "✅ All secrets configured!"
echo ""

# List secrets to verify
echo "📋 Configured secrets:"
gh secret list

echo ""
echo "🎉 Done! You can now push to trigger CI/CD:"
echo ""
echo "  git add ."
echo "  git commit -m 'chore: setup deployment'"
echo "  git push origin develop"
echo ""
echo "Monitor deployment:"
echo "  gh run watch"
echo "  # or visit: https://github.com/$REPO/actions"
