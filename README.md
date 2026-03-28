# report-publisher

This console application is used for creating following objects in Redash:
* datasource
* group
* dashboard
* widget
* visualization
* query
* snippet

It can also create custom roles and excerpt templates in PostgreSQL database

### Related components:
* PostgreSQL database for data persistence
* `report-exporter` - to export dashboards from Redash instance

### Usage
###### optional arguments
* `roles` - program argument(s) - used to create all objects associated with role (group, datasource, user in DB), ex.: `[--roles...]`
* `reports` - program argument(s) - used to create all objects associated with reports (queries, visualizations, widgets, dashboards), ex.: `[--reports...]`
* `snippets` - program argument(s) - used to create snippets, ex.: `[--snippets...]`
* `excerpts` - program argument(s) - used to populate `excerpt_template` table in DB with templates for excerpts, ex.: `[--excerpts...]`
* `admin` - program argument(s) - used to create datasource for `admin` role, ex.: `[--admin...]`
* `auditor` - program argument(s) - used to create role, group and datasource for `auditor` role, ex.: `[--auditor...]`

### Local development:
###### Prerequisites:
* Postgres database is configured and running
* Redash instance is configured and running
* Files you want imported to Redash are placed on the same level as packaged application or root level if application is ran from IDE (examples can be found in `src\main\test\resources` or in `reports` folder)

###### Configuration:
1. Check `src/main/resources/application.yaml` and customize database settings and Redash settings if needed (properties spring.datasource.url, username, password and redash.url, api-key for Redash)

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
2. Add optional arguments to program run arguments, depending what Redash objects you want created
3. Run application with your favourite IDE or via `java -jar ...` with jar file, created above

### License
report-publisher is Open Source software released under the Apache 2.0 license.