/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
class MockClientFailOnSend extends MockClient {
  public MockClientFailOnSend() {
    super(false, true, false);
  }
}
