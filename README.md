Ever dreamed of becoming the strongest like Satoru Gojo from Jujutsu Kaisen? This plugin brings his iconic Limitless cursed technique to Minecraft!

![image](https://github.com/user-attachments/assets/9b628bd9-f92e-4d21-bf58-3f8039309d89)
## Experience:
- **Infinity** — Become untouchable! Anything approaching you slows down infinitely (inspired by Zeno's paradox), making attacks never reach you.
- **Hollow Technique: Purple** — The ultimate destructive force! Combine Blue and Red to fire an unavoidable purple sphere that obliterates everything in its path, creating a massive zone of imaginary mass erasure.
- **Cursed Technique Lapse: Blue** — Generate a blue cursed energy sphere that attracts all entities in a radius, dealing continuous damage. Target an entity to pull it straight to you, or aim at a block to place an attracting sphere there.
- **Cursed Technique Reversal: Red** — Unleash repulsive red cursed energy to blast all entities away in a radius, or target one entity to repel it forcefully.
​
## How to Use:
- All abilities are triggered with `SHIFT + LEFT CLICK` (after enabling them).
- Switch between abilities with `SHIFT + MIDDLE MOUSE BUTTON` (scroll wheel click).
- Open the GUI to toggle abilities on/off: /limitless or /lm → Opens the Limitless GUI. Click any ability icon to enable/disable it.
- Reload config: `/limitless reload` (admin only).

## Placeholders API Support:
- `%limitless_infinity_state%`
- `%limitless_purple_state%`
- `%limitless_red_state%`
- `%limitless_blue_state%`

![image](https://github.com/user-attachments/assets/31b13b0c-d8e2-4634-813e-280ae6d081d7)
## Commands
- `/limitless` Open GUI for toggle ability
- `/limitless reload` Reload configurations

## Permissions
- limitless.admin:
    description: Allows administration of the plugin (reload)
    default: op
- limitless.use:
    description: Allows opening the Limitless GUI
    default: true
- limitless.ability.*:
    description: Allows usage of all Limitless abilities
    default: op
    children:
      limitless.ability.purple: true
      limitless.ability.infinity: true
      limitless.ability.blue: true
      limitless.ability.red: true
- limitless.ability.purple:
    description: Allows usage of Purple ability
    default: op
- limitless.ability.infinity:
    description: Allows usage of Infinity ability
    default: op
- limitless.ability.blue:
    description: Allows usage of Blue ability
    default: op
- limitless.ability.red:
    description: Allows usage of Red ability
    default: op
- limitless.ability.purple.bypasssaturation:
    description: Bypass saturation cost for Purple ability
    default: op
- limitless.ability.infinity.bypasssaturation:
    description: Bypass saturation cost for Infinity ability
    default: op
- limitless.ability.blue.bypasssaturation:
    description: Bypass saturation cost for Blue ability
    default: op
- limitless.ability.red.bypasssaturation:
    description: Bypass saturation cost for Red ability
    default: op
