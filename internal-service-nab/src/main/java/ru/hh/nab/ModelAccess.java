package ru.hh.nab;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class ModelAccess {
  private final EntityManagerFactory hiber;

  ModelAccess(EntityManagerFactory hiber) {
    this.hiber = hiber;
  }

  public <T, E extends Throwable> T perform(ModelCheckedAction<T, E> action) throws E {
    EntityManager s = hiber.createEntityManager();
    try {
      EntityTransaction tx = s.getTransaction();
      tx.begin();
      try {
        return action.perform(s);
      } finally {
        tx.commit();
      }
    } finally {
      s.close();
    }
  }

  public <T> T perform(ModelAction<T> action) {
    EntityManager s = hiber.createEntityManager();
    try {
      EntityTransaction tx = s.getTransaction();
      tx.begin();
      try {
        return action.perform(s);
      } finally {
        tx.commit();
      }
    } finally {
      s.close();
    }
  }
}