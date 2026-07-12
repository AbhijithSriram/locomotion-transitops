:: Backend
mkdir backend
mkdir backend\src
mkdir backend\src\main
mkdir backend\src\main\java
mkdir backend\src\main\java\com
mkdir backend\src\main\java\com\transitops

mkdir backend\src\main\java\com\transitops\domain
mkdir backend\src\main\java\com\transitops\domain\auth
mkdir backend\src\main\java\com\transitops\domain\vehicle
mkdir backend\src\main\java\com\transitops\domain\driver
mkdir backend\src\main\java\com\transitops\domain\trip
mkdir backend\src\main\java\com\transitops\domain\maintenance
mkdir backend\src\main\java\com\transitops\domain\finance
mkdir backend\src\main\java\com\transitops\domain\sync

mkdir backend\src\main\java\com\transitops\simulation
mkdir backend\src\main\java\com\transitops\simulation\engine
mkdir backend\src\main\java\com\transitops\simulation\route
mkdir backend\src\main\java\com\transitops\simulation\movement
mkdir backend\src\main\java\com\transitops\simulation\wear
mkdir backend\src\main\java\com\transitops\simulation\ws

mkdir backend\src\main\java\com\transitops\common
mkdir backend\src\main\java\com\transitops\common\dto
mkdir backend\src\main\java\com\transitops\common\enums
mkdir backend\src\main\java\com\transitops\common\events

mkdir backend\src\main\resources

mkdir backend\src\test
mkdir backend\src\test\java
mkdir backend\src\test\java\com
mkdir backend\src\test\java\com\transitops
mkdir backend\src\test\java\com\transitops\domain
mkdir backend\src\test\java\com\transitops\simulation

type nul > backend\pom.xml
type nul > backend\src\main\resources\application.yml

:: Web Dashboard
mkdir web-dashboard
mkdir web-dashboard\css
mkdir web-dashboard\js
mkdir web-dashboard\pages

type nul > web-dashboard\index.html
type nul > web-dashboard\js\api.js
type nul > web-dashboard\js\ws.js
type nul > web-dashboard\js\map.js
type nul > web-dashboard\js\dashboard.js

type nul > web-dashboard\pages\vehicles.html
type nul > web-dashboard\pages\drivers.html
type nul > web-dashboard\pages\trips.html
type nul > web-dashboard\pages\reports.html

:: Android Driver
mkdir android-driver
mkdir android-driver\app
mkdir android-driver\app\src
mkdir android-driver\app\src\main
mkdir android-driver\app\src\main\java
mkdir android-driver\app\src\main\java\com
mkdir android-driver\app\src\main\java\com\transitops
mkdir android-driver\app\src\main\java\com\transitops\driver

mkdir android-driver\app\src\main\java\com\transitops\driver\data
mkdir android-driver\app\src\main\java\com\transitops\driver\data\local
mkdir android-driver\app\src\main\java\com\transitops\driver\data\remote
mkdir android-driver\app\src\main\java\com\transitops\driver\data\sync

mkdir android-driver\app\src\main\java\com\transitops\driver\ui
mkdir android-driver\app\src\main\java\com\transitops\driver\ui\login
mkdir android-driver\app\src\main\java\com\transitops\driver\ui\trip
mkdir android-driver\app\src\main\java\com\transitops\driver\ui\checklist
mkdir android-driver\app\src\main\java\com\transitops\driver\ui\incident

mkdir android-driver\app\src\main\java\com\transitops\driver\di

:: Docs
mkdir docs

type nul > docs\api-contract.md
type nul > docs\events-contract.md
type nul > docs\enums.md
