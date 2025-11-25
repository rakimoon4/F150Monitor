# Launcher Icon Setup

The app needs launcher icons to display properly on your phone's home screen and app drawer.

## Quick Solution (Recommended)

Use Android Studio's built-in Image Asset tool:

1. In Android Studio, right-click on `app` folder
2. Select: New → Image Asset
3. Choose "Launcher Icons (Adaptive and Legacy)"
4. Select an image or use a built-in icon:
   - Try the "car" or "directions_car" icon
   - Or use any image of an F150
5. Adjust foreground/background colors to your preference
6. Click "Next" then "Finish"

This will automatically generate all required icon sizes for different screen densities.

## Manual Solution

If you prefer to add your own custom icon manually:

1. Create icons in these sizes:
   - mdpi: 48x48px
   - hdpi: 72x72px
   - xhdpi: 96x96px
   - xxhdpi: 144x144px
   - xxxhdpi: 192x192px

2. Name them `ic_launcher.png` and `ic_launcher_round.png`

3. Place in respective folders:
   - `app/src/main/res/mipmap-mdpi/`
   - `app/src/main/res/mipmap-hdpi/`
   - `app/src/main/res/mipmap-xhdpi/`
   - `app/src/main/res/mipmap-xxhdpi/`
   - `app/src/main/res/mipmap-xxxhdpi/`

## Default Behavior

The app will still build and work without custom icons - Android will use a default placeholder icon. This is fine for personal use, but adding a custom icon makes it look more professional.

## Icon Design Suggestions

For a 2006 F150 Monitor app, consider:
- F150 truck silhouette
- OBD port symbol
- Wrench + truck combination
- Speedometer/gauge design
- Engine symbol

Colors that work well:
- Ford blue: #003478
- Construction orange: #FF6F00
- Black and silver (professional)
