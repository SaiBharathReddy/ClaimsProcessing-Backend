# ClaimsProcessing-Backend
A Spring Boot APP that automates claim assessment by extracting structured data from lease, addendum and move-out documents using LLMs and evaluating coverage eligibility under SDI policy rules.

## Key Features
-> PDF text extraction using Apache PDFBox  
-> AI-powered structured extraction via Anthropic Claude API  
-> Deterministic rule evaluation aligned with SDI policy coverage/exclusion criteria  
-> Computes total approved payout and generates final decision JSON  

## Tech Stack
Language: Java 17  
Framework: Spring Boot  
AI Integration: Anthropic Claude Sonnet 4 API  

## How to Install and Run the Project
### Prerequisites  
Java 17 or higher  
An Anthropic API key  

## Setup
### Clone the repository
git clone https://github.com/SaiBharathReddy/ClaimsProcessing-Backend.git  
cd ClaimsProcessing-Backend/claims-processing  

## Set your Anthropic API key
export ANTHROPIC_API_KEY=sk-ant-.......  

## Build and run
mvn clean package  
mvn spring-boot:run  

The server starts at http://localhost:8080
