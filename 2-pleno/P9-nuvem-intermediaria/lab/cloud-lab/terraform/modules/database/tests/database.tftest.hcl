mock_provider "aws" {}

variables {
  name              = "t"
  subnet_ids        = ["subnet-1", "subnet-2"]
  security_group_id = "sg-1"
}

run "study_is_destroyable_and_still_private_and_encrypted" {
  command = plan
  assert {
    condition     = aws_db_instance.this.deletion_protection == false && aws_db_instance.this.skip_final_snapshot == true
    error_message = "the study environment must be destroyable"
  }
  assert {
    condition     = aws_db_instance.this.publicly_accessible == false && aws_db_instance.this.storage_encrypted == true
    error_message = "private and encrypted even in the study environment"
  }
  assert {
    condition     = aws_db_instance.this.manage_master_user_password == true
    error_message = "the password is created and rotated by RDS (Secrets Manager), never written in code or state"
  }
}

run "production_cannot_be_destroyed_by_accident" {
  command = plan
  variables {
    production = true
    multi_az   = true
  }
  assert {
    condition     = aws_db_instance.this.deletion_protection == true && aws_db_instance.this.skip_final_snapshot == false && aws_db_instance.this.final_snapshot_identifier == "t-db-final"
    error_message = "production: deletion protection ON and a final snapshot"
  }
  assert {
    condition     = aws_db_instance.this.multi_az == true && aws_db_instance.this.backup_retention_period >= 7
    error_message = "production: Multi-AZ and at least 7 days of automated backups"
  }
}

run "no_replica_by_default" {
  command = plan
  assert {
    condition     = length(aws_db_instance.replica) == 0
    error_message = "a replica costs an instance: only when asked"
  }
}

run "a_replica_has_no_credentials_of_its_own" {
  command = plan
  variables {
    create_replica = true
  }
  assert {
    condition     = length(aws_db_instance.replica) == 1 && aws_db_instance.replica[0].replicate_source_db == "t-db" && aws_db_instance.replica[0].password == null && aws_db_instance.replica[0].manage_master_user_password == null
    error_message = "the replica inherits data and users from the primary: no username/password here"
  }
  assert {
    condition     = aws_db_instance.replica[0].publicly_accessible == false && aws_db_instance.replica[0].storage_encrypted == true
    error_message = "a replica is as private and encrypted as the primary"
  }
}
