package dev.ensouled.core;

/**
 * The single enforcement point between a selected intention and the body's hands.
 *
 * <p>This is the narrower name the decision cycle uses; {@link AuthorityGateway} is the seam that a
 * production Axis implementation must satisfy. {@link Phase1GatePolicy} is a provisional
 * implementation of both, not the permanent authority layer.
 */
public interface ProceduralGate extends AuthorityGateway {
}
