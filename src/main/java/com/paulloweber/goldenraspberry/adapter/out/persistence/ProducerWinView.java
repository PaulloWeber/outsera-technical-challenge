package com.paulloweber.goldenraspberry.adapter.out.persistence;

/** Projection for the wins query, kept out of the domain. */
public interface ProducerWinView {
    String getProducer();

    int getYear();
}
