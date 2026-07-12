# TransitOps by Team Locomotion

TransitOps is an end-to-end transport ERP system built from the ground up to digitize vehicle dispatch, maintenance, and expense management. It is designed to solve real-world logistical challenges with a modern tech stack, providing fleet managers with unparalleled visibility and control over their operations.

### Live Demo
**Web Dashboard:** [https://locomotion-transitops.abhijith-sriram.in/](https://locomotion-transitops.abhijith-sriram.in/)

*(Note: The web dashboard provides a full god-mode view of the simulation, real-time map tracking, and analytics)*

### Core Features

- **Live Fleet Tracking & Dispatching:** A real-time WebSocket-powered dashboard that visually tracks dispatched vehicles on a live map, simulating realistic movement, speed fluctuations, and health degradation over time.
- **Comprehensive Vehicle & Driver Management:** Manage your entire fleet and workforce. Track vehicle health, odometers, load capacities, and driver availability and compliance statuses in real time.
- **Trip Simulation Engine:** A custom-built backend simulation engine that seamlessly generates telemetry data, dynamically updating trips, vehicle degradation, and map polylines without relying on external real-world feeds. 
- **Maintenance & Fuel Logging:** Integrated logging for vehicle repairs and fuel stops. Maintenance logs actively prevent vehicles from being dispatched while in the shop.
- **Reporting & Analytics:** An aggregation layer providing actionable insights into expenses, trip volumes, and vehicle utilization. Every module supports one-click CSV data exports for external bookkeeping.
- **Secure API Backend:** A robust Spring Boot REST API featuring JWT authentication, role-based access, and a unified data schema designed to gracefully scale.

### The Engineering Effort

We built TransitOps with a focus on delivering a production-ready architectural foundation during the hackathon. 

- The **Frontend** was engineered from scratch using Vanilla TypeScript, Vite, and Leaflet for geospatial tracking. We consciously avoided heavy UI frameworks to ensure maximum performance and maintain full control over the map rendering and WebSocket data streams.
- The **Backend** is powered by Java and Spring Boot, utilizing Spring Data JPA, WebSockets, and Spring Security. We implemented a sophisticated simulation tick-engine that concurrently manages state transitions, polyline encoding (using Google's algorithm), and live event broadcasting.
- We handled the full deployment pipeline, containerizing the system and securely exposing the backend API and frontend assets through Cloudflare tunnels, dealing with complex CORS and reverse-proxy setups along the way.

### Team Members
- Abhijith Sriram
- Mallesh Kumar K
- Srivathsan G
