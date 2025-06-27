# 🚀 Fiber - Framework Java pour API RESTful

Fiber est un framework Java moderne et élégant pour la création d'APIs RESTful, offrant une expérience de développement fluide avec une configuration minimale et des fonctionnalités puissantes.

## 📋 Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Installation](#installation)
- [Démarrage rapide](#démarrage-rapide)
- [Configuration](#configuration)
- [Authentification](#authentification)
- [Documentation API](#documentation-api)
- [Sécurité](#sécurité)

## ✨ Fonctionnalités

- 🛠️ Configuration simple et rapide
- 🔐 Système d'authentification intégré (JWT + OAuth2)
- 📝 Documentation API automatique
- 🛡️ Sécurité renforcée (Rate limiting, Audit logging)
- 🎯 Injection de dépendances
- 🔄 Support des bases de données via Architect

## 🚀 Installation

```xml
*Coming soon*
```

## 🎯 Démarrage rapide

```java
import sh.fyz.fiber.FiberServer;

public class Main {
    public static void main(String[] args) {
        // Créer et configurer le serveur
        FiberServer server = new FiberServer(8080);
        
        // Activer la documentation API
        server.enableDocumentation();
        
        // Démarrer le serveur
        server.start();
    }
}
```

## ⚙️ Configuration

### Configuration de la base de données

```java
Architect architect = new Architect()
    .setDatabaseCredentials(
        new DatabaseCredentials(
            new PostgreSQLAuth(
                "localhost",
                5432, 
                "database"),
            "username",
            "password",
            16));
architect.start();
```

## 🔐 Authentification

### Configuration de l'authentification

```java
// Initialiser les services
AuthService authService = new ImplAuthService(userRepository);
OAuthService oauthServiceExample = new OAuthService(authService, userRepository);

// Configurer le serveur
server.setAuthService(authService);
server.setOAuthService(oauthServiceExample);
```

### Exemple de contrôleur d'authentification

```java
@Controller("/auth")
public class AuthController {
    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    @RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
    public ResponseEntity<Map<String, String>> login(
            @Param("value") String value, 
            @Param("password") String password) {
        // Logique d'authentification
    }
    
    @RequestMapping(value = "/oauth/{provider}/login", method = RequestMapping.Method.GET)
    public void oauthLogin(@PathVariable("provider") String provider) {
        // Logique OAuth
    }
}
```

## 📝 Annotations

### Contrôleurs et Routage

#### `@Controller`
- **Description**: Définit une classe comme contrôleur REST
- **Usage**: Au niveau de la classe
- **Exemple**:
```java
@Controller("/api/exampleUsers")
public class UserController {
    // ...
}
```

#### `@RequestMapping`
- **Description**: Mappe une méthode à une route HTTP
- **Options**:
  - `value`: Le chemin de l'URL
  - `method`: La méthode HTTP (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
  - `description`: Description de l'endpoint pour la documentation
- **Usage**: Au niveau de la classe ou de la méthode
- **Exemple**:
```java
@RequestMapping(value = "/create", method = RequestMapping.Method.POST)
public ResponseEntity<User> createUser(@RequestBody User exampleUser) {
    // ...
}
```

#### `@PathVariable`
- **Description**: Extrait une variable de l'URL
- **Usage**: Au niveau des paramètres de méthode
- **Exemple**:
```java
@RequestMapping("/exampleUser/{id}")
public User getUser(@PathVariable("id") Long userId) {
    // ...
}
```

#### `@Param`
- **Description**: Récupère un paramètre de requête HTTP
- **Usage**: Au niveau des paramètres de méthode
- **Exemple**:
```java
@RequestMapping("/search")
public List<User> searchUsers(@Param("query") String searchQuery) {
    // ...
}
```

#### `@RequestBody`
- **Description**: Parse le corps de la requête HTTP en objet Java
- **Usage**: Au niveau des paramètres de méthode
- **Exemple**:
```java
@RequestMapping(value = "/update", method = RequestMapping.Method.PUT)
public User updateUser(@RequestBody User exampleUser) {
    // ...
}
```

### Gestion des Fichiers

#### `@FileUpload`
- **Description**: Gère l'upload de fichiers
- **Options**:
  - `maxSize`: Taille maximale du fichier en octets (-1 pour illimité)
  - `allowedMimeTypes`: Types MIME autorisés (tableau vide pour tous)
  - `maxChunkSize`: Taille maximale d'un chunk pour l'upload (-1 pour désactiver)
- **Exemple**:
```java
@RequestMapping(value = "/upload", method = RequestMapping.Method.POST)
public ResponseEntity<String> uploadFile(
    @FileUpload(maxSize = 5242880, allowedMimeTypes = {"image/jpeg", "image/png"}) 
    MultipartFile file
) {
    // ...
}
```

### Authentification et Sécurité

#### `@AuthenticatedUser`
- **Description**: Injecte l'utilisateur authentifié dans la méthode
- **Usage**: Au niveau des paramètres de méthode
- **Exemple**:
```java
@RequestMapping("/profile")
public User getProfile(@AuthenticatedUser User exampleUser) {
    return exampleUser;
}
```

#### `@RequireRole`
- **Description**: Restreint l'accès aux utilisateurs ayant un rôle spécifique
- **Usage**: Au niveau de la méthode
- **Exemple**:
```java
@RequireRole("ADMIN")
@RequestMapping("/admin/dashboard")
public Dashboard getAdminDashboard() {
    // ...
}
```

#### `@IdentifierField`
- **Description**: Marque les champs utilisés pour l'identification lors de la connexion
- **Usage**: Au niveau des champs de classe utilisateur
- **Exemple**:
```java
public class User {
    @IdentifierField
    private String email;
    
    @IdentifierField("username")
    private String login;
}
```

#### `@PasswordField`
- **Description**: Marque le champ utilisé comme mot de passe
- **Usage**: Au niveau du champ de classe utilisateur
- **Exemple**:
```java
public class User {
    @PasswordField
    private String password;
}
```

## 🛡️ Sécurité

### Rate Limiting

```java
@RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
public ResponseEntity<?> protectedEndpoint() {
    // Endpoint protégé
}
```

### Audit Logging

```java
@AuditLog(action = "USER_ACTION", logParameters = true, maskSensitiveData = true)
public ResponseEntity<?> auditedEndpoint() {
    // Action auditée
}
```

## 📚 Documentation API

La documentation API est automatiquement générée et accessible aux endpoints suivants :

- Interface utilisateur: `http://localhost:8080/docs/ui`
- Documentation brute: `http://localhost:8080/docs/api`

## 🔧 Bonnes pratiques

1. **Structure du projet**
   - Organisez vos contrôleurs par domaine fonctionnel
   - Séparez la logique métier des contrôleurs
   - Utilisez l'injection de dépendances

2. **Sécurité**
   - Activez le rate limiting sur les endpoints sensibles
   - Utilisez l'audit logging pour les actions importantes
   - Masquez les données sensibles dans les logs

3. **Documentation**
   - Documentez vos endpoints avec des annotations appropriées
   - Fournissez des exemples de requêtes et réponses
   - Maintenez la documentation à jour

## 📖 Exemples

Consultez le package `sh.fyz.fiber.example` pour des exemples complets d'implémentation, notamment :

- Configuration complète du serveur
- Authentification OAuth2
- Gestion des utilisateurs
- Contrôleurs REST
- Sécurité et audit

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :

1. Fork le projet
2. Créer une branche (`git checkout -b feature/amelioration`)
3. Commit vos changements (`git commit -am 'Ajout d'une fonctionnalité'`)
4. Push vers la branche (`git push origin feature/amelioration`)
5. Créer une Pull Request

## 📄 Licence

Fiber est distribué sous la licence MIT. Voir le fichier `LICENSE` pour plus d'informations. 
