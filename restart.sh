#!/bin/bash

echo "=== Reiniciando o servidor de Minecraft ==="
echo "Conectando ao servidor..."

# Configurações de conexão
SERVER_IP="181.215.45.238"
USER="root"
PASSWORD="devprime"
SERVER_DIR="/home/minecraft/server"

# Primeiro, vamos tentar descarregar o plugin (se possível)
echo "Descarregando o plugin PrimeLeagueClans..."
sshpass -p "$PASSWORD" ssh $USER@$SERVER_IP "cd $SERVER_DIR && screen -r -S minecraft -p 0 -X stuff 'plugman unload PrimeLeagueClans\n'"
sleep 2

# Desligando o servidor
echo "Desligando o servidor..."
sshpass -p "$PASSWORD" ssh $USER@$SERVER_IP "cd $SERVER_DIR && screen -r -S minecraft -p 0 -X stuff 'stop\n'"

# Tempo para o servidor desligar com segurança
echo "Aguardando 20 segundos para o servidor desligar completamente..."
sleep 20

# Verificar se o processo ainda está rodando, se estiver, matar
echo "Verificando se o servidor está totalmente desligado..."
sshpass -p "$PASSWORD" ssh $USER@$SERVER_IP "if pgrep -f 'java.*server.jar' > /dev/null; then pkill -SIGTERM -f 'java.*server.jar'; echo 'Processo do servidor encerrado.'; sleep 5; else echo 'Servidor já estava desligado.'; fi"

# Copiando o novo jar para o servidor
echo "Copiando o plugin para o servidor..."
sshpass -p "$PASSWORD" scp target/PrimeLeagueClans-1.0.jar $USER@$SERVER_IP:$SERVER_DIR/plugins/PrimeLeagueClans.jar

# Iniciando o servidor
echo "Iniciando o servidor..."
sshpass -p "$PASSWORD" ssh $USER@$SERVER_IP "cd $SERVER_DIR && screen -dmS minecraft bash -c 'java -Xms2G -Xmx6G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=50 -XX:+DisableExplicitGC -XX:TargetSurvivorRatio=90 -XX:+AggressiveOpts -jar server.jar nogui'"

echo "Aguardando 45 segundos para o servidor iniciar completamente..."
sleep 45

# Verificando logs do servidor para confirmar o status
echo "Verificando logs para confirmar o status do plugin..."
sshpass -p "$PASSWORD" ssh $USER@$SERVER_IP "cd $SERVER_DIR && grep -n 'PrimeLeagueClans' server.log | tail -50"

echo "=== Reinicialização concluída ===" 