# 🍩 DonutAuction

**DonutAuction** is a feature-rich, lightweight, and modern Auction House plugin for Minecraft servers. It allows players to buy and sell items securely with an intuitive GUI, transaction history, and configurable limits.

---

## ✨ Features

- **GUI-Based Interface**: User-friendly menu for browsing, buying, and selling.
- **Selling System**: Easily list items for sale with `/ah sell <price>`.
- **Buying System**: Instant purchase confirmation to prevent accidental buys.
- **Transaction History**: Track your sales and purchases with a detailed history log.
- **Sorting & Filtering**:
  - Sort by Highest Price, Lowest Price, Recently Listed, etc.
  - Category filters (Blocks, Tools, Combat, etc.) via `filter.yml`.
  - Search by item name.
- **Admin Controls**: Remove listings, reload configs, and manage the economy.
- **Configurable**: extensive `config.yml` for messages, sounds, limits, and GUI layouts.
- **Vault Support**: Seamless integration with server economy.

## 🚀 Installation

1.  **Prerequisites**:
    - Java 1.8 or higher.
    - A Spigot/Paper server (1.16+ recommended).
    - [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy provider (e.g., EssentialsX).

2.  **Build**:
    Clone the repository and run Maven:
    ```bash
    mvn clean package
    ```

3.  **Install**:
    - Drop the generated `DonutAuction-1.0-SNAPSHOT.jar` into your server's `plugins` folder.
    - Restart the server.

## 📖 Commands & Permissions

### Player Commands
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/ah` | Opens the main Auction House GUI. | `None` (Default) |
| `/ah sell <price>` | Lists the item in your hand for sale. | `None` (Default) |

### Admin Commands
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/donutauction reload` | Reloads `config.yml`, `filter.yml`, and `saves.yml`. | `donutauction.admin` |

## ⚙️ Configuration

The plugin uses three main configuration files:
- **`config.yml`**: Main settings (max price, max listings, messages, sounds, GUI layout).
- **`filter.yml`**: Define items for specific categories (e.g., Diamond Sword -> Combat).
- **`saves.yml`**: Stores active auctions and transaction history (do not edit manually while server is running).

## 🛠️ Compilation

This project is built using **Maven**.
To compile the project, ensure you have Maven installed and run:

```bash
mvn clean package
```

The output JAR will be located in the `target/` directory.

---

### 👨‍💻 Credits

**Project**: DonutAuction
**Fixed by**: h2ph
