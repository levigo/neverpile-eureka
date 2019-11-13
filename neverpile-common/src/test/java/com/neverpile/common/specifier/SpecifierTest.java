package com.neverpile.common.specifier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SpecifierTest {
  @Test
  public void testThat_specifierCanBeParsed() throws Exception {
    assertThat(Specifier.from("")).isEqualTo(new Specifier(new String[0], 0));

    assertThat(Specifier.from("foo")).isEqualTo(new Specifier("foo"));

    assertThat(Specifier.from("foo.bar")).isEqualTo(new Specifier("foo", "bar"));

    assertThat(Specifier.from(" foo.bar")).isEqualTo(new Specifier("foo", "bar"));

    assertThat(Specifier.from("foo.bar ")).isEqualTo(new Specifier("foo", "bar"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDotAtStartIsRejected() throws Exception {
    Specifier.from(".foo.bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDotAtEndIsRejected() throws Exception {
    Specifier.from("foo.bar.");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDuplicateDotIsRejected1() throws Exception {
    Specifier.from("foo..bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDuplicateDotIsRejected2() throws Exception {
    Specifier.from("foo\\\\..bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDuplicateDotIsRejected3() throws Exception {
    Specifier.from("foo..\\\\bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDuplicateDotIsRejected4() throws Exception {
    Specifier.from("foo...bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_specifierWithDuplicateDotIsRejected5() throws Exception {
    Specifier.from("foo..\\.bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_isolatedBackslashesAreRejected1() throws Exception {
    Specifier.from("foo\\bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_isolatedBackslashesAreRejected2() throws Exception {
    Specifier.from("\\foo.bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_isolatedBackslashesAreRejected3() throws Exception {
    Specifier.from("foo.bar\\");
  }

  @Test
  public void testThat_escapingWorks() throws Exception {
    assertThat(Specifier.from("foo\\.bar")).isEqualTo(new Specifier("foo.bar"));

    assertThat(Specifier.from("foo\\\\.bar")).isEqualTo(new Specifier("foo\\", "bar"));

    assertThat(Specifier.from("foo\\\\\\.bar")).isEqualTo(new Specifier("foo\\.bar"));

    assertThat(Specifier.from("foo\\\\\\\\.bar")).isEqualTo(new Specifier("foo\\\\", "bar"));

    assertThat(Specifier.from("foo\\..bar")).isEqualTo(new Specifier("foo.", "bar"));

    assertThat(Specifier.from("foo.\\.bar")).isEqualTo(new Specifier("foo", ".bar"));

    assertThat(Specifier.from("foo\\\\bar")).isEqualTo(new Specifier("foo\\bar"));

    assertThat(Specifier.from("foo\\\\bar.baz")).isEqualTo(new Specifier("foo\\bar", "baz"));
  }

  @Test
  public void testThat_specifierCanBeReassenbled() throws Exception {
    assertThat(Specifier.from("foo").asString()).isEqualTo("foo");
    assertThat(Specifier.from("foo.bar").asString()).isEqualTo("foo.bar");

    assertThat(Specifier.from("foo\\\\bar").asString()).isEqualTo("foo\\\\bar");
    assertThat(Specifier.from("foo\\.bar").asString()).isEqualTo("foo\\.bar");
  }

  @Test
  public void testThat_specifierElementAccessWorks() throws Exception {
    Specifier s = Specifier.from("foo.bar.baz");

    assertThat(s.head()).isEqualTo("foo");
    assertThat(s.element(1)).isEqualTo("bar");
    assertThat(s.element(2)).isEqualTo("baz");
  }

  @Test
  public void testThat_specifierLengthIsCorrect() throws Exception {
    assertThat(Specifier.from("").length()).isEqualTo(0);
    assertThat(Specifier.from("foo").length()).isEqualTo(1);
    assertThat(Specifier.from("foo.bar").length()).isEqualTo(2);

    assertThat(Specifier.from("").hasMore()).isFalse();
    assertThat(Specifier.from("foo").hasMore()).isFalse();
    assertThat(Specifier.from("foo.bar").hasMore()).isTrue();

    assertThat(Specifier.from("").empty()).isTrue();
    assertThat(Specifier.from("foo").empty()).isFalse();
    assertThat(Specifier.from("foo.bar").empty()).isFalse();
  }

  @Test
  public void testThat_specifierTailIsCorect() throws Exception {
    assertThat(Specifier.from("foo").suffix()).isEqualTo(Specifier.from(""));
    assertThat(Specifier.from("foo.bar").suffix()).isEqualTo(Specifier.from("bar"));
    assertThat(Specifier.from("foo.bar.baz").suffix()).isEqualTo(Specifier.from("bar.baz"));

    assertThat(Specifier.from("foo.bar.baz").suffix(Specifier.from("foo"))).isEqualTo(Specifier.from("bar.baz"));
    assertThat(Specifier.from("foo.bar.baz").suffix(Specifier.from("foo.bar"))).isEqualTo(Specifier.from("baz"));
  }

  @Test
  public void testThat_specifierPrefixMatchingIsCorect() throws Exception {
    assertThat(Specifier.from("foo").startsWith("foo")).isTrue();
    assertThat(Specifier.from("foo.bar").startsWith("foo")).isTrue();
    assertThat(Specifier.from("foo.bar.baz").startsWith("foo")).isTrue();

    assertThat(Specifier.from("foo.bar.baz").startsWith("bar")).isFalse();

    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("foo"))).isTrue();
    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("foo.bar"))).isTrue();
    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("foo.bar.baz"))).isTrue();

    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("bar.baz"))).isFalse();
    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("baz"))).isFalse();
    assertThat(Specifier.from("foo.bar.baz").startsWith(Specifier.from("foo.bar.baz.yada"))).isFalse();
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testThat_indexCheckingOnLeftWorks() throws Exception {
    Specifier.from("foo.bar.baz").element(-1);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testThat_indexCheckingOnRightWorks() throws Exception {
    Specifier.from("foo.bar.baz").element(3);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testThat_indexCheckingOnLeftWithOffsetWorks() throws Exception {
    Specifier.from("foo.bar.baz").suffix().element(-1);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testThat_indexCheckingForEmptySpecWorks() throws Exception {
    Specifier.from("").head();
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testThat_tailForEmptySpecWorks() throws Exception {
    Specifier.from("").suffix();
  }
}
