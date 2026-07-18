# Silicon Valley: The Tech Cartel

A JavaFX-based strategic board game inspired by the core ideas of Catan, reimagined in the competitive ecosystem of Silicon Valley.

Players act as technology founders who collect resources, establish companies, form partnerships, trade in a dynamic market, and upgrade their businesses to dominate the tech industry. The first player to reach the required number of Victory Points wins the game.

## Project Overview

Silicon Valley: The Tech Cartel is a turn-based strategy board game implemented in Java. It adapts settlement-building and resource-management mechanics into a technology-startup setting.

Instead of villages and cities, players build MVPs, expand through Partnerships, and upgrade successful ventures into Unicorns. Resources represent critical assets in a technology ecosystem, including Talent, Cloud, Patent, Data, and Capital.

The project provides a JavaFX graphical interface for match configuration, board interaction, game actions, trading, save/load workflows, undo/redo history navigation, and competition against AI-controlled players.

## Course Information

| Item | Details |
|---|---|
| Course | Advanced Programming |
| University | Ferdowsi University of Mashhad |
| Academic Term | Winter 1404 – Spring 1405 |
| Language | Java 25 |
| Build Tool | Maven |
| Primary UI Technology | JavaFX + FXML |

## Key Features

- JavaFX graphical user interface built with FXML and a dedicated UI controller
- Catan-inspired strategic gameplay adapted to a startup and technology theme
- Configurable matches for 2 to 4 total players
- Support for human-controlled and AI-controlled players
- Setup dialogs for selecting total players, AI opponents, and AI difficulty
- AI modes: `NORMAL` and `ADVANCED`
- Asynchronous AI turns to keep the JavaFX interface responsive
- Board representation based on Sectors, Vertices, and Edges
- Resource production through dice rolls and sector activation numbers
- Company development through MVP placement, Partnership construction, and Unicorn upgrades
- Dynamic market for buying and selling resources using Capital
- Player-to-player resource trading through a dedicated JavaFX dialog
- Auditor crisis handling on valid board sectors
- Victory and stalemate detection
- Asynchronous save/load operations
- Undo/redo support through game-state snapshots
- Event log and live player statistics
- Custom exception hierarchy for rule violations and persistence errors

## Technologies and Dependencies

| Technology | Purpose |
|---|---|
| Java 25 | Main programming language and runtime |
| JavaFX | Desktop graphical user interface framework |
| FXML | Declarative layout definition for JavaFX |
| Maven | Dependency management and build automation |
| IntelliJ IDEA | Recommended development environment |

## Requirements

- JDK 25
- Maven 3.9+
- Internet connection for first-time Maven dependency resolution
- Java IDE such as IntelliJ IDEA (recommended)

Verify installed versions:

```bash
java --version
mvn --version
```

`java --version` should report Java 25.

## Installation

```bash
git clone https://github.com/ItIsZEFiX/silicon-valley-ap-spring-05.git
cd silicon-valley-ap-spring-05
```

On the first build, Maven downloads required dependencies from `pom.xml`.

## Running the Application

Run the JavaFX game through Maven:

```bash
mvn clean javafx:run
```

After startup, configure:
1. Total number of players (2 to 4)
2. Number of AI-controlled players
3. AI difficulty (`NORMAL` or `ADVANCED`) if AI is enabled

`Launcher` is the entry point and delegates startup to the JavaFX application class.

## Running from IntelliJ IDEA

1. Open the project root as a Maven project
2. Import dependencies from `pom.xml`
3. Set project SDK to JDK 25
4. Wait for Maven indexing and dependency resolution
5. In Maven tool window, run `javafx:run`

You can also create a Maven run configuration with:

```text
javafx:run
```

## Gameplay Overview

The game includes a setup phase followed by alternating turns.

### Setup Phase

1. Optional Founder Role selection
2. Initial placement round 1 (clockwise): one MVP and one connected Partnership per player (free)
3. Initial placement round 2 (reverse order): second MVP and Partnership
4. Initial resources from sectors adjacent to the second MVP

### Turn Sequence

1. Roll two dice and produce resources based on activation number
2. Perform actions in any order if costs and requirements are satisfied
3. End turn

Available actions:
- Build MVP
- Upgrade MVP to Unicorn
- Build Partnership
- Buy Resource (market)
- Sell Resource (market)
- Trade with Players
- Place Auditor (when required)

## Construction Rules and Costs

| Structure | Victory Points | Cost | Requirements |
|---|---:|---|---|
| MVP | 1 | 1 Capital, 1 Talent, 1 Cloud, 1 Data | Eligible vertex connected to player's network |
| Unicorn | 2 | 3 Data, 2 Cloud | Upgrade from an existing MVP |
| Partnership | 0 direct VP | 1 Capital, 1 Patent | Must connect to player's MVP/Unicorn/Partnership network |

Partnerships expand the build network and may contribute to longest-network scoring.

## Core Game Concepts

### Resources

| Resource | Main Uses |
|---|---|
| Capital | Building MVPs and Partnerships |
| Talent | Building MVPs |
| Cloud | Building MVPs and upgrading to Unicorns |
| Patent | Building Partnerships |
| Data | Building MVPs and upgrading to Unicorns |

### Board Entities

| Entity | Description |
|---|---|
| GameMap | Board containing sectors, vertices, and edges |
| Sector | Resource-producing area with activation number; may contain Auditor |
| Vertex | Location for MVP or Unicorn placement |
| Edge | Location for Partnership placement |
| MVP | Company structure worth 1 VP |
| Unicorn | Upgraded MVP worth 2 VP |
| Partnership | Network connection enabling expansion |
| Player | Human or AI participant |
| Market | Dynamic buy/sell resource market |
| Dice | Determines sector activation |
| FounderRole | Optional role with unique advantage |
| Auditor | Blocks production on its current sector |

## Dynamic Market

Players can buy or sell resources during their action phase.

| Action | Description |
|---|---|
| Buy Resource | Spend Capital to obtain one unit of a selected resource |
| Sell Resource | Give one unit of a selected resource to receive Capital |

Prices are dynamic: buying tends to increase price, selling tends to reduce price.

## Auditor Crisis

When a sector contains the Auditor:
- That sector produces no resources when activated
- Adjacent MVPs and Unicorns receive no resource from that sector
- Other sectors with the same activation number still produce normally

Auditor placement is strategically impactful and can disrupt high-value production.

## Founder Roles

At game start, players may choose a unique optional Founder Role with a special advantage, starting bonus, or game-specific ability. Roles are intended to increase variety and strategic diversity.

## Victory and Scoring

A player wins by reaching the configured Victory Point target (typically 10 VP).

Primary scoring sources:
- MVP: 1 VP
- Unicorn: 2 VP
- Longest Partnership network: additional scoring benefit where applicable
- Other role/game-specific effects

A Unicorn replaces an MVP at the same vertex, changing that location from 1 VP to 2 VP.

A stalemate may end the match as a draw if no player can mathematically reach the target and no productive action remains.

## AI and Strategy Pattern

The AI subsystem uses the Strategy pattern:

```text
AIStrategy (interface)
▲
│
AdvancedAIStrategy
```

- `AIStrategy` defines the AI decision contract
- Concrete strategies implement specific behavior profiles
- `GameController` delegates AI turns through this interface
- AI turns are processed asynchronously for UI responsiveness

## Save, Load, Undo, and Redo

### Save and Load

`SaveLoadManager` handles:
- Saving current game state
- Loading saved game state
- Asynchronous file operations to avoid UI blocking

Persisted state includes players, resources, structures, board state, turn state, market state, and Auditor position.

### Game History

`GameHistoryManager` supports:
- Undo
- Redo
- Snapshot-based state recovery

## Exception Handling

| Exception | Purpose |
|---|---|
| `GameException` | Base type for game-related failures |
| `InsufficientResourcesException` | Action attempted without required resources |
| `InvalidPlacementException` | Invalid structure placement on board |
| `IllegalGameActionException` | Action violates turn/rule state |
| `CorruptSaveFileException` | Save file is invalid, incomplete, malformed, or corrupted |

The JavaFX controller catches these exceptions and presents user-facing feedback.

## Architecture

| Layer | Main Components | Responsibility |
|---|---|---|
| View | `main-layout.fxml`, `GameApp`, JavaFX controls | Render state and receive input |
| UI Controller | `MainController` | Bind UI events to game actions and refresh UI |
| Game Controller | `GameController` | Rule enforcement, turns, production, scoring, game flow |
| Model | `Player`, `GameMap`, `Sector`, `Vertex`, `Edge`, `Market`, structures | Domain state and behavior |
| Services | `SaveLoadManager`, `GameHistoryManager` | Persistence and history |
| AI | `AIStrategy`, `AdvancedAIStrategy` | AI move selection |

## Project Structure

```text
src/
└── com.techcartel.siliconvalley/
    ├── controller/
    │   ├── GameController.java
    │   ├── MainController.java
    │   ├── SaveLoadManager.java
    │   ├── GameHistoryManager.java
    │   ├── AIStrategy.java
    │   └── AdvancedAIStrategy.java
    ├── model/
    │   ├── Player.java
    │   ├── GameMap.java
    │   ├── Sector.java
    │   ├── Vertex.java
    │   ├── Edge.java
    │   ├── Market.java
    │   ├── Dice.java
    │   ├── CompanyStructure.java
    │   ├── MVP.java
    │   ├── Unicorn.java
    │   ├── Partnership.java
    │   ├── ResourceType.java
    │   ├── StructureType.java
    │   ├── FounderRole.java
    │   ├── AIType.java
    │   └── GameConstants.java
    ├── exception/
    │   ├── GameException.java
    │   ├── InsufficientResourcesException.java
    │   ├── InvalidPlacementException.java
    │   ├── IllegalGameActionException.java
    │   └── CorruptSaveFileException.java
    ├── view/
    │   ├── GameApp.java
    │   ├── Launcher.java
    │   ├── ConsoleApp.java
    │   ├── ConsoleUI.java
    │   └── main-layout.fxml
    └── resources/
        └── main-layout.fxml
```

## Application Entry Points

| Class | Responsibility |
|---|---|
| `Launcher` | Primary JavaFX launcher |
| `GameApp` | JavaFX `Application` initialization and FXML loading |
| `MainController` | UI event handling and state synchronization |
| `ConsoleApp` | Console entry point for development/debugging |
| `ConsoleUI` | Console interaction helper |

Graphical startup flow:

```text
Launcher
   ↓
GameApp
   ↓
main-layout.fxml
   ↓
MainController
   ↓
GameController
```

## Console Mode (Debug Only)

Console mode is retained for development and debugging.

```java
public static void main(String[] args) {
    new ConsoleUI().start();
}
```

The JavaFX interface is the primary user-facing mode.

## Important Constants

Shared configuration values are centralized in `GameConstants` (for example: player limits, VP target, structure costs, market baseline values, and longest-network scoring values) to keep rule values consistent and maintainable.

## Team Contributions

### Sajjad Nemati Farooji

- Designed and implemented the custom exception hierarchy
- Developed the AI subsystem (`AIStrategy`, `AdvancedAIStrategy`, `AIType`)
- Implemented AI-related logic in `Player` and `GameController`
- Worked on AI turn synchronization
- Implemented `SaveLoadManager`

### Reza Nosrati

- Implemented non-AI core logic in `GameController` and `Player`
- Implemented `GameHistoryManager`
- Developed the game-domain model and related utilities/enums (except `AIType`)
- Developed JavaFX app layer, FXML, UI controllers, startup classes, and integration

## Known Limitations and Future Improvements

- Expand unit and integration test coverage
- Improve AI planning and long-term strategy quality
- Refine JavaFX UX, visual feedback, and accessibility
- Evolve market behavior with richer dynamic mechanics
- Add multiplayer/network play if scope is expanded

## Repository

Source code:

[https://github.com/ItIsZEFiX/silicon-valley-ap-spring-05](https://github.com/ItIsZEFiX/silicon-valley-ap-spring-05)
