package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.model.AbstractInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractAdapter<T extends AbstractInfo> {
  private T info;
  private final Class<T> infoClass;

  public AbstractAdapter(Class<T> infoClass) {
    this(infoClass, true);
  }

  public AbstractAdapter(Class<T> infoClass, boolean initialize) {
    this.infoClass = infoClass;
    if (initialize) {
      initialize();
    }
  }

  protected void initialize() {

  }

  public boolean canAdapt(AbstractInfo info) {
    return infoClass.isInstance(info);
  }

  abstract public T adapt(T info);

  @SuppressWarnings("unchecked")
  public <I extends AbstractInfo> void setInfo(I info) {
    // Verify that the provided info is of the correct type
    if (!canAdapt(info)) {
      throw new IllegalArgumentException("Cannot adapt info of type " + info.getClass().getName() + " to " + this.getClass().getName());
    }
    this.info = (T) info;
  }
}
