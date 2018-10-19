---
title: 2018 Training Workshop Schedule
last_updated: 2018-10-18
sidebar: tdsTutorial_sidebar
toc: false
permalink:  workshop2018.html
---

## 2018-10-26

### 8:30 (30 minutes) Coffee and continental breakfast

### 9:00 (30 minutes) Welcome and Logistics / TDS Overview (Sean)
* Introduction of Unidata staff
* Review of schedule
* Why use TDS? (pdf)
* TDS Resources: Web page, Reference, FAQ

### 9:30 (30 minutes) Participant Introductions (Sean)
* Name, organization, how you use TDS.

### 10:00 (30 minutes) Getting Started With the TDS: Local Test Server Setup part 1 (Jen)
* This section covers basic installation and configuration of Tomcat, JDK and the TDS for a local test server.
  * [Installing Java and Tomcat](/install_java_tomcat.html)
  * [Tomcat Directory Structure: Quick Tour](/tomcat_dir_structure_qt.html)
  * [Running Tomcat](/running_tomcat.html)
  * [Tomcat Log Files](/tomcat_log_files.html)
  * [Tomcat (Server-Level) Configuration Files](/tomcat_configuration_files.html)

### 10:30 (15 minutes) Break

### 10:45 (45 minutes) Getting Started With the TDS: Local Test Server Setup part 2 (Jen)
* This section covers basic installation and configuration of Tomcat, JDK and the TDS for a local test server.
  * [Deploying the TDS](/deploying_the_tds.html)
  * [Tomcat manager Application](/tomcat_manager_app.html)
  * [TDS Remote Management](/remote_management_ref.html)
  * [Next Steps: Where To Go From Here](/where_to_go_from_here.html)

### 11:30 (1 hour) Lunch in FL-2 Cafeteria

### 12:30 (45 minutes) Configuring TDS (Part 1) : Client and Server Catalog Overview (Sean)
* [TDS Client Catalog Overview](/basic_client_catalog.html)
* [Basic TDS Configuration Catalogs](/basic_config_catalog.html)

### 1:15 (30 minutes) Tomcat Monitoring and Debugging (Jen)
* This section covers log files generated by Tomcat and the TDS for the purposes of monitoring and debugging:
  * [Logs!](/tds_monitoring_and_debugging.html)
  * [Tomcat Access Logs](/tds_monitoring_and_debugging.html#tomcat-access-logs)
  * [Log Files Generated by the TDS](/tds_monitoring_and_debugging.html#log-files-generated-by-the-tds)

### 1:45 (15 minutes) TDS Monitoring and Debugging (Sean)
* Looking at logs on the server using Remote Management (a.k.a. debug page)
* [Using the TdsMonitor tool](/using_the_tdsmonitor_tool.html)

### 2:00 (15 minutes) Break

### 2:15 (30 minutes) Configuring TDS (Part 2) : service, metadata, datasetScan (Sean)
* TDS Configuration Catalogs
* Troubleshooting Configuration Catalogs

### 2:45 (15 minutes) Configuring TDS (Part 3) : threddsConfig.xml (Sean)
* [Basic threddsConfig.xml](/basic_tds_configuration.html)
* [threddsConfig Reference](/tds_config_ref.html)
* Services
* enabling writing netCDF-4 files

### 3:00 (30 minutes) Docker (Julien)

### 3:30 (30 minutes) DAP Protocol Services (Dennis)
* OPeNDAP DAP2 and DAP4 Protocol Services (pdf)
 
### 4:00 Discussion and Questions

### Day One Finish

## 2018-10-27

### 8:30 (30 minutes) Coffee and continental breakfast

### 9:00 (15 minutes) ToolsUI Demo (Sean)
* Running ToolsUI
* Viewer, CoordinateSystem, IOSP, and FeatureTypes

### 9:30 (30 minutes) Conventions (Ethan)

### 10:00 (15 minutes) Break

### 10:15 (15 minutes) NcML modifications (Sean)
* [Basic NcML tutorial](/tds_basic_ncml_tutorial.html)

### 10:30 (30 minutes) NcML aggregation (Sean)
* [NcML Aggregation](/tds_ncml_aggregation.html)
* [NcML Aggregation Example Problems](/ncml_aggregation_examples.html)
* NcML Aggregations vs Feature Collections (pdf)

### 11:00 (30 minutes) [Using NcML in the TDS](/using_ncml_in_the_tds.html)

### 11:30 (1 hour) Lunch FL-2 Cafeteria

### 12:30 (30 minutes) Tour of Services (Sean)
* Data Discovery
  * Data discovery systems: (pdf)
  * ncIso
  * exercise- how can we increase our ncISO score?
  * Attribute Convention for Data Discovery
* WMS
  * WMS configure, reference (pdf)
* NCSS
  * Netcdf Subset Service configure, reference (pdf)

### 1:00 (45 minutes) Advanced TDS Configuration (Sean)
* [FeatureCollections](/feature_collections_ref.html)
* [FMRC Tutorial](/fmrc_tutorial.html)
* Point Feature Collections

### 1:45 (15 minutes) Break

### 2:00 (1 hour) GRIB Feature Collections (Sean)
* GRIB Collection Examples
* [GRIB Feature Collection Tutorial](/grib_feature_collections.html)
* GRIB Index redirection
* [TDM](/tdm_ref.html)

### 3:00 (30 minutes) Resources and Contributing (Sean)
* [Source](https://github.com/unidata/thredds){:target="_blank"} on GitHub
* [Issue](https://github.com/unidata/thredds/issues){:target="_blank"} Tracking with GitHub
* Maven artifacts on [Nexus](https://artifacts.unidata.ucar.edu/){:target="_blank"} 
* CDM/TDS Nightly Build/Test System (full test suite)
* Continuous Integration on Travis (subset of tests)
* Static code analysis on sonarcloud
* Email Lists: thredds@unidata.ucar.edu; netcdf-java@unidata.ucar.edu
* Support: support-thredds@unidata.ucar.edu; support-netcdf-java@unidata.ucar.edu
* [TdsConfig on github](https://github.com/unidata/TdsConfig){:target="_blank"}
* [Upgrading to 5.0](/upgrade_to_5.html)
   
### 4:00 Open Discussion / Participant Systems / Participant feedback (Sean)

### Day Two Finish