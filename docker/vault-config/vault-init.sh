#!/bin/sh

set -e

echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    echo "Vault is not ready yet. Waiting..."
    sleep 2
done

echo "Vault is ready. Checking if already initialized..."

if vault status | grep -q "Initialized.*true"; then
    echo "Vault is already initialized"
    export VAULT_TOKEN=root-token
else
    echo "Initializing Vault..."
    vault operator init -key-shares=1 -key-threshold=1 -format=json > /vault/init-keys.json
    
    UNSEAL_KEY=$(cat /vault/init-keys.json | jq -r '.unseal_keys_b64[0]')
    ROOT_TOKEN=$(cat /vault/init-keys.json | jq -r '.root_token')
    
    echo "Unsealing Vault..."
    vault operator unseal $UNSEAL_KEY
    
    export VAULT_TOKEN=$ROOT_TOKEN
fi

echo "Authenticating with Vault..."
vault auth $VAULT_TOKEN

echo "Setting up secrets engines..."
vault secrets enable -path=secret/ kv-v2 || echo "KV secrets engine may already be enabled"

echo "Storing application secrets..."
vault kv put secret/config-server username=config-user password=config-pass
vault kv put secret/postgres host=postgres port=5432 database=barakah_main username=barakah_admin password=barakah_admin_pass
vault kv put secret/keycloak admin-username=admin admin-password=admin
vault kv put secret/kafka bootstrap-servers=kafka:9092
vault kv put secret/redis host=redis port=6379

echo "Vault initialization and configuration completed successfully!"
