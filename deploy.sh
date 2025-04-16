#!/bin/bash

echo "=== Iniciando deploy do PrimeLeagueClans ==="

# Compilar o projeto
echo "Compilando projeto..."
mvn clean package

# Verificar se a compilação foi bem-sucedida
if [ $? -ne 0 ]; then
    echo "Erro na compilação!"
    exit 1
fi

echo "Compilação concluída com sucesso!"

# Caminho do arquivo JAR compilado
JAR_FILE="target/PrimeLeagueClans-1.0.jar"

# Configurações do servidor
SERVER_USER="root"
SERVER_IP="181.215.45.238"
SERVER_PORT="22"
SERVER_PLUGIN_DIR="/home/minecraft/server/plugins"
SERVER_PASSWORD="devprime"

# Copiar JAR para o servidor
echo "Enviando plugin para o servidor..."
sshpass -p "$SERVER_PASSWORD" scp -P $SERVER_PORT $JAR_FILE $SERVER_USER@$SERVER_IP:$SERVER_PLUGIN_DIR/PrimeLeagueClans.jar

# Verificar se o envio foi bem-sucedido
if [ $? -ne 0 ]; then
    echo "Erro ao enviar plugin para o servidor!"
    exit 1
fi

# Renomear o arquivo no servidor (já não é necessário, pois definimos o nome diretamente)
echo "Plugin enviado com sucesso!"

# Recarregar plugin no servidor
echo "Recarregando plugin no servidor..."
sshpass -p "$SERVER_PASSWORD" ssh -p $SERVER_PORT $SERVER_USER@$SERVER_IP "cd /home/minecraft/server && screen -r -X stuff 'plugman reload PrimeLeagueClans\n'"

echo "=== Deploy concluído com sucesso! ===" 