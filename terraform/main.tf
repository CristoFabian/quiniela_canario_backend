# ============================================================================
# Terraform: Despliegue de Quinielas API en Oracle Cloud Free Always
# ============================================================================
#
# REQUISITOS:
#   - Terraform >= 1.0
#   - OCI CLI configurado
#   - Cuenta OCI Free Always (Always Free eligible)
#
# USO:
#   terraform init
#   terraform plan -var-file="terraform.tfvars"
#   terraform apply -var-file="terraform.tfvars"
#
# ============================================================================

terraform {
  required_version = ">= 1.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 5.0"
    }
  }
}

provider "oci" {
  tenancy_ocid = var.tenancy_ocid
  user_ocid    = var.user_ocid
  private_key  = file(var.private_key_path)
  fingerprint  = var.fingerprint
  region       = var.region
}

# ─────────────────────────────────────────────────────────────────────────────
# Variables de Entrada
# ─────────────────────────────────────────────────────────────────────────────

variable "tenancy_ocid" {
  description = "OCID del tenancy de OCI"
  type        = string
}

variable "user_ocid" {
  description = "OCID del usuario de OCI"
  type        = string
}

variable "private_key_path" {
  description = "Ruta a la llave privada OCI API"
  type        = string
  default     = "~/.oci/oci_api_key.pem"
}

variable "fingerprint" {
  description = "Fingerprint de la llave pública OCI API"
  type        = string
}

variable "region" {
  description = "Región de OCI (ej: us-ashburn-1, us-phoenix-1)"
  type        = string
  default     = "us-ashburn-1"
}

variable "compartment_ocid" {
  description = "OCID del compartment donde desplegar"
  type        = string
}

variable "admin_username" {
  description = "Nombre de usuario del administrador inicial"
  type        = string
  sensitive   = true
}

variable "admin_password" {
  description = "Contraseña del administrador inicial (mín 10 caracteres)"
  type        = string
  sensitive   = true
  validation {
    condition     = length(var.admin_password) >= 10
    error_message = "La contraseña debe tener mínimo 10 caracteres."
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Datos
# ─────────────────────────────────────────────────────────────────────────────

# Obtener imagen de Ubuntu 22.04 disponible en Always Free
data "oci_core_images" "ubuntu" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04 Minimal"
  sort_by                  = "TIMECREATED"
  filter {
    name   = "display_name"
    values = ["Canonical-Ubuntu-22.04-*"]
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# VCN (Virtual Cloud Network)
# ─────────────────────────────────────────────────────────────────────────────

resource "oci_core_vcn" "quinielas_vcn" {
  compartment_id = var.compartment_ocid
  cidr_block     = "10.0.0.0/16"
  display_name   = "quinielas-vcn"
}

# Subnet Pública
resource "oci_core_subnet" "public_subnet" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.quinielas_vcn.id
  cidr_block                 = "10.0.1.0/24"
  display_name               = "public-subnet"
  map_public_ip_on_launch    = true
  prohibit_internet_ingress  = false
  prohibit_public_ip_on_vnic = false
}

# Internet Gateway
resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.quinielas_vcn.id
  display_name   = "internet-gateway"
  enabled        = true
}

# Route Table
resource "oci_core_route_table" "route_table" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.quinielas_vcn.id
  display_name   = "route-table"

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

# Asociar Route Table con Subnet
resource "oci_core_route_table_attachment" "route_table_attachment" {
  subnet_id      = oci_core_subnet.public_subnet.id
  route_table_id = oci_core_route_table.route_table.id
}

# Security Group
resource "oci_core_network_security_group" "nsg" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.quinielas_vcn.id
  display_name   = "quinielas-nsg"
}

# Regla de entrada: HTTP
resource "oci_core_network_security_group_security_rule" "http" {
  network_security_group_id = oci_core_network_security_group.nsg.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"

  tcp_options {
    destination_port_range {
      min = 80
      max = 80
    }
  }
}

# Regla de entrada: HTTPS
resource "oci_core_network_security_group_security_rule" "https" {
  network_security_group_id = oci_core_network_security_group.nsg.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"

  tcp_options {
    destination_port_range {
      min = 443
      max = 443
    }
  }
}

# Regla de entrada: SSH
resource "oci_core_network_security_group_security_rule" "ssh" {
  network_security_group_id = oci_core_network_security_group.nsg.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"

  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

# Regla de entrada: Puerto de aplicación (8080)
resource "oci_core_network_security_group_security_rule" "app" {
  network_security_group_id = oci_core_network_security_group.nsg.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"

  tcp_options {
    destination_port_range {
      min = 8080
      max = 8080
    }
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Instancia de Compute (VM.Standard.E2.1.Micro - Always Free)
# ─────────────────────────────────────────────────────────────────────────────

resource "oci_core_instance" "quinielas_app" {
  compartment_id      = var.compartment_ocid
  availability_domain = data.oci_core_images.ubuntu.images[0].launch_options[0].capacity_reservation_id == null ? data.oci_identity_availability_domains.ads.availability_domains[0].name : ""
  display_name        = "quinielas-api-server"
  shape               = "VM.Standard.E2.1.Micro"

  image_id = data.oci_core_images.ubuntu.images[0].id

  create_vnic_details {
    subnet_id              = oci_core_subnet.public_subnet.id
    nsg_ids                = [oci_core_network_security_group.nsg.id]
    skip_source_dest_check = false
  }

  # Script de inicialización: Instalar Java, MySQL, y desplegar la app
  user_data = base64encode(templatefile("${path.module}/cloud-init.sh", {
    admin_username = var.admin_username
    admin_password = var.admin_password
  }))

  metadata = {
    ssh_authorized_keys = file(var.ssh_public_key_path)
  }

  depends_on = [
    oci_core_internet_gateway.igw
  ]
}

# Datos de Availability Domains
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.compartment_ocid
}

# ─────────────────────────────────────────────────────────────────────────────
# MySQL Database (Always Free)
# ─────────────────────────────────────────────────────────────────────────────

resource "oci_mysql_mysql_db_system" "quinielas_db" {
  compartment_id = var.compartment_ocid
  display_name   = "quinielas-db"

  db_name                = "quinielas_deportivas"
  admin_username         = "admin"
  admin_password         = var.admin_password # Usar mismo password (NO en producción)

  shape_name  = "MySQL.8.0.32-1.0.Micro" # Always Free eligible
  subnet_id   = oci_core_subnet.public_subnet.id

  backup_policy {
    is_enabled = true
  }

  is_highly_available = false # Not available in Always Free

  configuration_id = null

  depends_on = [
    oci_core_subnet.public_subnet
  ]
}

# ─────────────────────────────────────────────────��───────────────────────────
# Outputs
# ─────────────────────────────────────────────────────────────────────────────

output "instance_public_ip" {
  value       = oci_core_instance.quinielas_app.public_ip
  description = "IP pública de la instancia"
}

output "app_url" {
  value       = "http://${oci_core_instance.quinielas_app.public_ip}:8080"
  description = "URL de la aplicación"
}

output "admin_login" {
  value       = "Usuario: ${var.admin_username}"
  description = "Credenciales del admin"
  sensitive   = true
}

output "db_hostname" {
  value       = oci_mysql_mysql_db_system.quinielas_db.endpoints[0].hostname
  description = "Hostname de la base de datos"
}

output "db_ip" {
  value       = oci_mysql_mysql_db_system.quinielas_db.ip_address
  description = "IP de la base de datos"
}

