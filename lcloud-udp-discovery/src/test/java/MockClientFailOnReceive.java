/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

class MockClientFailOnReceive extends MockClient {
  public MockClientFailOnReceive() {
    super(false, false, true);
  }
}
