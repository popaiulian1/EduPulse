package com.edupulse.ingestion.entities;

import java.util.UUID;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * A large parent chunk: the context handed to the LLM. Its id is assigned up front because the
 * child chunks in the vector store reference it, so this entity implements Persistable to make
 * Spring Data insert it directly instead of checking for an existing row first.
 */
@Getter
@Setter
@Entity
@Table(name = "document_parents")
public class DocumentParent implements Persistable<UUID> {

  @Id
  private UUID id;

  private UUID documentId;
  private int pageNumber;
  private int parentIndex;
  private String content;

  @Transient
  private boolean isNew = true;

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }
}
