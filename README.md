# NestJS

`https://docs.nestjs.com/`

```bash
# Installer les dépendances
npm install

# Lancer le serveur
npm run start

# Générer un module / service / controller
nest g {module/service/controller} {nom}
```

# Prisma

`https://www.prisma.io/docs/guides/frameworks/nestjs`

```bash
# Migration
npx prisma migrate dev --name init

# Générer le client
npx prisma generate

# Lancer UI
npx prisma studio
```

# PostgreSQL

```bash
# Lancer le CMD
sudo -u postgres psql

# Quitter le CMD
\q

# Créer utilisateur
CREATE USER takos WITH PASSWORD 'takos123';

# Permissions utilisateur
GRANT ALL PRIVILEGES ON DATABASE prog_mobile_projet TO takos;

# Créer database
CREATE DATABASE prog_mobile_projet;
```