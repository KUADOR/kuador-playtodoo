import { useId } from 'react'
import type { InputHTMLAttributes, SelectHTMLAttributes } from 'react'
import { ChevronDown, CircleAlert } from 'lucide-react'
import a11y from '../a11y.module.css'
import styles from './Field.module.css'

export interface FieldOption {
  value: string
  label: string
}

interface FieldBaseProps {
  /** Always visible above the control; it is also its accessible name. */
  label: string
  /** Optional hint shown below the control. */
  help?: string
  /** What happened and what to do, without blaming. Marks the control as invalid. */
  error?: string
  disabled?: boolean
}

type ManagedAttributes = 'id' | 'aria-describedby' | 'aria-invalid' | 'disabled'

interface TextControlProps extends Omit<InputHTMLAttributes<HTMLInputElement>, ManagedAttributes> {
  kind: 'text'
}

interface SelectControlProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, ManagedAttributes> {
  kind: 'select'
  options: FieldOption[]
  /** Empty first option shown while nothing is selected. */
  placeholder?: string
}

export type TextFieldProps = FieldBaseProps & TextControlProps
export type SelectFieldProps = FieldBaseProps & SelectControlProps
export type FieldProps = TextFieldProps | SelectFieldProps

interface ManagedControlProps {
  id: string
  disabled: boolean
  'aria-describedby'?: string
  'aria-invalid'?: true
}

export function Field({ label, help, error, disabled = false, ...control }: FieldProps) {
  const controlId = useId()
  const helpId = `${controlId}-help`
  const errorId = `${controlId}-error`
  const describedBy = [help && helpId, error && errorId].filter(Boolean).join(' ')

  const managed: ManagedControlProps = {
    id: controlId,
    disabled,
    'aria-describedby': describedBy || undefined,
    'aria-invalid': error ? true : undefined,
  }

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={controlId}>
        {label}
      </label>
      {control.kind === 'select' ? <SelectControl {...control} managed={managed} /> : <TextControl {...control} managed={managed} />}
      {help && (
        <p id={helpId} className={styles.help}>
          {help}
        </p>
      )}
      {error && (
        <p id={errorId} className={styles.error}>
          <CircleAlert className={styles.icon} aria-hidden="true" />
          <span>{error}</span>
        </p>
      )}
    </div>
  )
}

function controlClassName(...extra: (string | undefined)[]): string {
  return [styles.control, a11y.focusRing, ...extra].filter(Boolean).join(' ')
}

function TextControl({ kind: _kind, managed, className, type = 'text', ...rest }: TextControlProps & { managed: ManagedControlProps }) {
  return <input {...rest} {...managed} type={type} className={controlClassName(className)} />
}

function SelectControl({
  kind: _kind,
  managed,
  options,
  placeholder,
  className,
  ...rest
}: SelectControlProps & { managed: ManagedControlProps }) {
  return (
    <div className={styles.selectWrapper}>
      <select {...rest} {...managed} className={controlClassName(styles.select, className)}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <ChevronDown className={`${styles.icon} ${styles.selectIcon}`} aria-hidden="true" />
    </div>
  )
}
