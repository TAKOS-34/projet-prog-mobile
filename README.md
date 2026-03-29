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
npx prisma migrate dev

# Clean database
prix prisma migrate reset

# Générer le client
npx prisma generate

# Interface admin
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

# Créer database
CREATE DATABASE prog_mobile_projet;

# Permissions utilisateur
GRANT ALL PRIVILEGES ON DATABASE prog_mobile_projet TO takos;
```