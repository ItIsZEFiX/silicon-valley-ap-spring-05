# Silicon Valley: The Tech Cartel

A Catan-style board game (5x5 sector grid, MVP/Unicorn/Partnership structures,
dynamic market, regulatory crisis, founder roles). This build uses a **plain
terminal UI** — no JavaFX, no Swing.

## How to run

The project still contains `module-info.java`, `GameApp.java` and
`Launcher.java` (the original JavaFX entry point) untouched, as requested.
Since those require JavaFX on the module path, the simplest way to run the
**console version** is to compile everything *except* those three files:

```bash
cd src/main/java
javac -d out $(find . -name "*.java" \
  ! -name "module-info.java" ! -name "GameApp.java" ! -name "Launcher.java")
java -cp out com.techcartel.siliconvalley.ConsoleApp
```

(If your IDE already has JavaFX configured on the module path, you can also
just run `ConsoleApp.main()` directly — it doesn't touch JavaFX at all.)

## Package structure

```
com.techcartel.siliconvalley
 ├─ ConsoleApp.java         entry point for the terminal UI
 ├─ GameApp.java / Launcher.java   (untouched, JavaFX — not used by the console build)
 ├─ model/                  Sector, Vertex, Edge, Player, Market, Dice,
 │                          CompanyStructure (abstract) + MVP / Unicorn / Partnership
 ├─ controller/              GameController (core engine), AIStrategy (rule-based AI),
 │                          SaveLoadManager (serialization on a background thread)
 ├─ view/                    ConsoleUI (the whole terminal front end)
 ├─ util/                    GameConstants, ResourceType, FounderRole, StructureType
 └─ exception/               GameException (base) and its subclasses:
                             InvalidPlacementException, InsufficientResourcesException,
                             IllegalGameActionException, CorruptSaveFileException
```

## What was added on top of the original code

- **`CompanyStructure` hierarchy** (`MVP`, `Unicorn`, `Partnership`) with
  `produce()` / `getVictoryPoints()` overridden, as required. `Vertex` and
  `Edge` keep their original public API but are now backed by real structure
  objects instead of a bare `owner` field.
- **Dynamic victory points** on `Player`, computed from owned structures
  (+ role penalty, + longest-network bonus) instead of a stored counter.
- **Market price history**: prices rise 1 on purchase (cap 6) and fall 1
  after 3 consecutive rounds with no purchase (floor 2), per the manual.
- **Placement rules** in `GameMap`: the 2-vertex distance rule for new
  companies, and the "must connect to your own network" rule for
  Partnerships/MVPs after setup.
- **Setup phase**: snake-order free MVP + Partnership placement, then
  starting resources from each player's second MVP.
- **Regulatory Crisis (roll of 7)**: over-the-limit players auto-discard
  half their cards (simplified: no card-by-card picker in this simple UI),
  then the current player places the Auditor, which blocks production on
  its sector.
- **Founder roles**: Hacker CEO (cheaper market buys), Tech Guru/CTO
  (cheaper Unicorn upgrade), VC-Funded (starting Capital + higher crisis
  limit) — all wired into `GameController`, not just declared.
- **Longest network** bonus (2 VP, min length 3), computed via DFS over each
  player's Partnership edges.
- **Exception handling**: every illegal action (bad placement, insufficient
  resources, wrong phase, corrupt save file) throws one of the custom
  `GameException` subclasses instead of silently failing; `ConsoleUI`
  catches and reports them so the game never crashes.
- **Save/Load**: whole `GameController` graph is `Serializable`; I/O always
  runs on its own background thread (`SaveLoadManager`), never on the main
  thread.
- **Thread management**: a rule-based `AIStrategy` can run any player's
  turn (setup or main phase) automatically. AI turns execute on a
  dedicated background thread (`ExecutorService` inside `GameController`);
  every method that touches shared state (`market`, `gameMap`, `players`,
  turn order) is `synchronized`, so a human action and an AI turn can never
  corrupt state even if they were to overlap.
- **No magic numbers**: every cost, limit, and threshold lives in
  `GameConstants`.

## Known simplifications (kept deliberately simple per your request)

- Regulatory-crisis discards are automatic (always drop from your largest
  pile) rather than an interactive card-by-card picker.
- Trading with another player is a direct "I give X, you give Y" exchange
  typed in by the current player (no separate offer/accept UI).
- The AI (`AIStrategy`) is intentionally simple/greedy (rule-based), which
  satisfies the base "AI" extra-credit tier, not the smarter Minimax tier.

## Filling in for submission

Remember the manual asks for a short README/PDF with: how to run, a UML
class overview, a description of the game logic, and the division of work
between the two team members — add those details before zipping for
upload (`SiliconValley_[StudentID1]_[StudentID2].zip`).
