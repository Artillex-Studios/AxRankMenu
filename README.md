# AxRankMenu

AxRankMenu is a LuckPerms rank menu with transactional prices and extensible rank requirements.

**Bug reports and feature requests:** https://github.com/Artillex-Studios/Issues

**Support:** https://dc.artillex-studios.com/

![AxRankMenu](https://github.com/Artillex-Studios/AxRankMenu/assets/52270269/0a1208b0-9f91-4127-ab70-448b2a4ae381)

## Requirements

Each rank can define an `ALL` or `ANY` requirement set. Existing `price` and `currency`
fields remain supported and are included in the same transaction. `ALL` consumes every
successful consumable entry. `ANY` selects the first successful entry in configuration
order and consumes it only when that selected entry is consumable.

```yaml
skyseed:
  rank: skyseed
  server: ""
  price: 0
  currency: Vault
  slot: 10

  requirements:
    mode: ALL
    entries:
      money:
        type: currency
        currency: ExcellentEconomy-money
        amount: 50000
        operator: ">="
        consume: true
      gems:
        type: currency
        currency: ExcellentEconomy-gem
        amount: 250
        operator: ">="
        consume: true
      island-level:
        type: superior-skyblock
        metric: island-level
        amount: 25
        operator: ">="
        fail-if-no-island: true
        consume: false
      playtime:
        type: statistic
        statistic: PLAY_ONE_MINUTE
        unit: hours
        amount: 12
        operator: ">="
        consume: false
      xp-level:
        type: minecraft-xp-level
        amount: 25
        operator: ">="
        consume: true
      permission:
        type: permission
        permission: axrankmenu.progression
        has: true
      current-group:
        type: luckperms-group
        group: default
        has: true
      votes:
        type: placeholder
        placeholder: "%superbvote_votes%"
        value-type: number
        operator: ">="
        expected: 10
        abbreviations: true
      progression:
        type: expression
        strict: true
        variables:
          votes: "%superbvote_votes%"
          quests: "%axquests_completed%"
        expression: "(votes >= 20 || quests >= 10) && !empty(votes)"
        failed-part: progression

  item:
    type: LIME_BANNER
    name: "&#00FF00&lSKYSEED"
    lore:
      - " &7Balance: &f%axrankmenu_requirement_skyseed_money_current% / %axrankmenu_requirement_skyseed_money_required%"
      - " &7XP: &f%axrankmenu_requirement_skyseed_xp-level_current% / %axrankmenu_requirement_skyseed_xp-level_required%"
      - " &7Requirements: &f%axrankmenu_requirements_skyseed_met_count%/%axrankmenu_requirements_skyseed_total_count%"
  buy-actions:
    - "[MESSAGE] &#33FF33You purchased %rank%."
    - "[CLOSE] menu"
```

Supported providers:

- `currency`: every registered AxRankMenu currency hook. ExcellentEconomy and
  CoinsEngine accept `Provider-currencyId`, such as `ExcellentEconomy-gem`.
- `minecraft-xp-level` and `minecraft-xp-points`: check and safely consume XP.
- `statistic`: `PLAY_ONE_MINUTE` in `ticks`, `minutes`, `hours`, or `days`.
- `superior-skyblock`: `island-level`, `island-worth`, `island-bank`,
  `island-members`, and a named `island-upgrade`. Only `island-bank` can be consumed.
- `permission` and `luckperms-group`: side-effect-free access checks.
- `placeholder`: number, string, and boolean comparisons.
- `expression`: compiled, restricted expressions with parentheses, comparisons,
  boolean operators, safe string functions, variables, and direct placeholders.

Placeholder checks are read-only. Command-backed consumption requires both
`consume-actions` and `rollback-actions`:

```yaml
custom-token:
  type: placeholder
  placeholder: "%custom_tokens%"
  value-type: number
  operator: ">="
  expected: 100
  consume-actions:
    - "[CONSOLE] tokens take %player% 100"
  rollback-actions:
    - "[CONSOLE] tokens give %player% 100"
```

The command dispatcher can only confirm that a command was accepted, not that an
external plugin committed its operation. Prefer a registered API provider whenever
one exists.

## Transaction behavior

AxRankMenu reads every requirement again at purchase time, acquires a per-player
purchase lock, and performs no deduction if any required check fails. Consumable
requirements are applied sequentially. If a later deduction or LuckPerms save fails,
completed deductions are rolled back in reverse order. Buy actions only run after
LuckPerms confirms the rank update.

Other plugins can register a requirement provider through:

```java
AxRankMenu.getRequirementRegistry().register(myProvider);
```

Providers explicitly advertise consumption support and return structured evaluation,
consumption, and rollback results.
