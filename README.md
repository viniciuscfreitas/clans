# Plugin de Clãs para Spigot 1.5.2

Um plugin completo de clãs para Spigot 1.5.2, totalmente baseado em GUI e com suporte a MySQL/SQLite.

## 📌 Características

- Sistema de clãs completo e moderno
- Interface gráfica intuitiva (GUI)
- Suporte a MySQL e SQLite
- Sistema de permissões hierárquico
- Sistema de banco para clãs
- Sistema de logs administrativos
- Backup automático de dados
- Cache para melhor performance
- Compatibilidade com Vault

## 📌 Requisitos

- Spigot 1.5.2
- Java 8
- Vault (para economia)
- PermissionsEx (recomendado)

## 📌 Instalação

1. Baixe o arquivo .jar do plugin
2. Coloque-o na pasta `plugins` do seu servidor
3. Reinicie o servidor
4. Configure o plugin em `plugins/ClansPlugin/config.yml`

## 📌 Comandos

- `/clans` - Abre o menu principal de clãs
- `/clans staff` - Abre o menu de staff (requer permissão)

## 📌 Permissões

### Permissões de Staff
- `clans.admin` - Acesso total às funções administrativas
- `clans.staff` - Acesso às funções de staff

### Permissões de Clã
- `clans.fundador` - Permissões de fundador
- `clans.lider` - Permissões de líder
- `clans.sublider` - Permissões de sub-líder
- `clans.membro` - Permissões de membro
- `clans.recruta` - Permissões de recruta

## 📌 Configuração

O plugin possui um arquivo de configuração completo em `plugins/ClansPlugin/config.yml`:

```yaml
settings:
  clan-creation-cost: 10000.0
  max-members: 50
  cache-time: 300
  auto-backup: 60

gui:
  main-menu-title: "&8[&6Clãs&8]"
  members-menu-title: "&8[&6Membros do Clã&8]"
  settings-menu-title: "&8[&6Configurações do Clã&8]"
  staff-menu-title: "&8[&6Menu de Staff&8]"

messages:
  clan-join: "&aVocê entrou no clã &f%clan%"
  clan-leave: "&cVocê saiu do clã &f%clan%"
  no-permission: "&cVocê não tem permissão para fazer isso!"
  clan-not-found: "&cClã não encontrado!"
  player-not-found: "&cJogador não encontrado!"
  clan-created: "&aClã criado com sucesso!"
  clan-deleted: "&aClã deletado com sucesso!"
  staff-join: "&c[Staff] &f%player% entrou no clã &f%clan%"
  staff-leave: "&c[Staff] &f%player% saiu do clã &f%clan%"

logging:
  level: INFO
  date-format: "yyyy-MM-dd HH:mm:ss"
```

## 📌 Funcionalidades

### Sistema de Clãs
- Criar clãs
- Gerenciar membros
- Sistema de cargos
- Sistema de permissões
- Sistema de banco
- Sistema de logs

### Interface Gráfica
- Menu principal
- Menu de clã
- Menu de membros
- Menu de configurações
- Menu de staff

### Sistema de Staff
- Monitoramento de clãs
- Gerenciamento de membros
- Visualização de logs
- Backup manual
- Limpeza de cache

## 📌 Suporte

Para suporte, entre em contato através do GitHub ou Discord.

## 📌 Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes. 