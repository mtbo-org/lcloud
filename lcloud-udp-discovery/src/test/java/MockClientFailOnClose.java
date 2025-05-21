/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
class MockClientFailOnClose extends MockClient {
  public MockClientFailOnClose() {
    super(true, false, false);
  }
}
